#!/usr/bin/env node

/**
 * SamMikrotik ISP Financial Management - Offline License Token Generator CLI
 * 
 * Usage:
 *   node generate_license.js <hardwareDeviceId> [durationMonths] [customerName]
 * 
 * Examples:
 *   node generate_license.js SAM-A1B2-C3D4-E5F6 12 "Al-Noor Net"
 *   node generate_license.js SAM-A1B2-C3D4-E5F6 0 "Al-Noor Net (Lifetime)"
 *   node generate_license.js "*" 1 "Universal Trial Key"
 */

const crypto = require("crypto");

// RSA-2048 Private Key (PKCS#8 DER in Base64)
// KEEP THIS STRICTLY SECRET ON THE ADMIN BACKEND! NEVER EMBED IN CLIENT APPS.
const PRIVATE_KEY_DER_BASE64 = 
  "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDNAqwycTvrmUTLSloj14v5dyLbyXoi6vIoPxgqVMwEDbI2wwOGAqtxYkCJAZP8FtvhCRBkdTeGGtliX7Rd0cMEjEsDkNmMh//5U5FpgfqgsjODBWDjAeqn1H4J3vCmpo5hZ2/GGaUyPa659DaQZ6SDeJuPP7YwQcGiXAfC0OkwfwYvlmcegYHOgJdh2Zlp1k78aYtgSF7lgJAX9D7DnrDI2JAl2V0vxprISqy8iiAlnwtaIpqMBr8CJL4reyntpAQQ+ixFFNs9EEQSZn7ne9zy/DTb/Kj+eWMiVChLbpYALZRV5WUhcuvexQeApOhqLDXzo8P97PvN9jiAPcMFGhG5AgMBAAECggEAEYKqmJMJY4HaLrmcDLcAEqpgBNZ8Tc00KnHb1jRwnzatmSB7sDo8JdlsL7I9zz4DQSutWxc+oIAHP0VzZMFiNITocgEw7mA+DIVcbzRAkZyG6n2kHTTwXli3pzoujd1oszMgMz3E2WS2rtVdb7oagsw4uo8zjYL1pLDS94sxJQjH/KLkXJHMsBo0FCoY7K6XFwnx2U1Lu5ixsjZSPlqR1yCzrWoA78xbvs7IV06mfdlGshkKFlrTmtzQSfAb0lovLJ8/OXVc4/kLFr49aGLxOU7aXfPzIIYec+vo0+cv2bw3Ci9k1j9OYa+gt30bMVQ0vQ+xo66nD2PoxjspuLYEnQKBgQDpQCMhSBHCe3nTs9r4YH98q7DWj8sVCHQV2kzKQhWRhpsy2piWlRU4/pXGdHOYAWatdvTPG5rf+a/7Yy2KEXfc8TrhPYiC9YL2QpZSHhw6n8KKFuzi+7lTkJSmyY3ScP6a6A6d4dnX/nfk+WoPdzNi56LX9JFJncCPgH8scugg7wKBgQDhAW3UpEpHrNYgVXZLPE7ri+3qINIu6o0Q94tLAp5wsXQhoxsqvSWoMcWGQLHhxXDaXyiqAVqxTHUqYTuKUFW7GHDtq7Q9JO9Sq1Vb2RrmQhZE4n/Y1MrPKOFUXKKX05j6sPiPGrMpoV8uXitfqN2gDqbPfzrqwi01CxbXJv4n1wKBgDjrjucbSEI2kadI98GvAs1wtInXs05hrrp6eEzPTaYivBLv51av8OrkrbYtStwoU183bUxxRx0rOHfXXxCXbLG9KIo40uxqb1gbLcXCVU81F2tNZueJWFN9L9l3zCUzr94rjQ2YROoBzk5RU0GM7DxRnJqosx/M4GXkjKRTMjR/AoGAF7lM2UTRSCljElmwdVJQh+ljFnKLdsLoSMew+iG2ju9G7V0DOkhjNg65YuRjXQiyU3QBF5u48kS7fdJusrCxuV5kWcrm4COK0E6cW2jSu0BwlKbFEBmvoJQQ3hI+Wn/G+3e5XW1VQqhvHrZzIEzvwTaKRAqILcR3e8ZFjD3iQlUCgYBTbsAy9YrGZ9YbCfXysPNhiJWTxiLJtxTiiWKrZRlzWPi4uU+Ysj0F+/7b9KIzk8AWsKqbFCrSLC3H4eEe1UbJZeXhcIp/ZE0pvgKbU1gXK8pufskJ/uoyzvkH6/OcsEVzQ+LWn//3swbS1okv9VYcy+2y27Tmo0zV4obC5bDU5w==";

function generateLicenseToken(deviceId, months = 12, customerName = "") {
  if (!deviceId || typeof deviceId !== "string" || deviceId.trim().length === 0) {
    throw new Error("Device ID is required.");
  }

  const cleanDeviceId = deviceId.trim().toUpperCase();
  const now = Date.now();
  const numMonths = parseInt(months, 10);
  
  let expirationEpochMs = -1;
  if (numMonths > 0) {
    // 30 days per month
    expirationEpochMs = now + (numMonths * 30 * 24 * 60 * 60 * 1000);
  }

  const payload = {
    deviceId: cleanDeviceId,
    tier: "PRO",
    expirationEpochMs: expirationEpochMs > 0 ? expirationEpochMs : -1,
    issuedAtEpochMs: now,
    customerName: customerName.trim(),
    issuer: "SamMikrotik License Authority"
  };

  const payloadJson = JSON.stringify(payload);
  const payloadBase64 = Buffer.from(payloadJson, "utf8").toString("base64");

  const privKeyPem = `-----BEGIN PRIVATE KEY-----\n${PRIVATE_KEY_DER_BASE64}\n-----END PRIVATE KEY-----`;
  const signer = crypto.createSign("SHA256");
  signer.update(Buffer.from(payloadJson, "utf8"));
  const signatureBase64 = signer.sign(privKeyPem, "base64");

  const token = `${payloadBase64}.${signatureBase64}`;
  return {
    token,
    payload,
    expiryFormatted: expirationEpochMs > 0 ? new Date(expirationEpochMs).toISOString().split("T")[0] : "Lifetime (دائم)"
  };
}

if (require.main === module) {
  const args = process.argv.slice(2);
  if (args.length === 0) {
    console.log("Usage: node generate_license.js <hardwareDeviceId> [durationMonths] [customerName]");
    console.log("Example: node generate_license.js SAM-9A2B-C4D8 12 'Net ISP'");
    process.exit(1);
  }

  const deviceId = args[0];
  const months = args[1] !== undefined ? parseInt(args[1], 10) : 12;
  const customer = args[2] || "عميل مميز";

  try {
    const result = generateLicenseToken(deviceId, months, customer);
    console.log("\n========================================================");
    console.log("🔑 SamMikrotik PRO License Key Generated Successfully!");
    console.log("========================================================");
    console.log(`Device ID:   ${result.payload.deviceId}`);
    console.log(`Customer:    ${result.payload.customerName}`);
    console.log(`Tier:        ${result.payload.tier}`);
    console.log(`Duration:    ${months === 0 ? "Lifetime" : months + " Months"}`);
    console.log(`Expires On:  ${result.expiryFormatted}`);
    console.log("--------------------------------------------------------");
    console.log("ACTIVATION KEY (Copy to Android App):");
    console.log(result.token);
    console.log("========================================================\n");
  } catch (err) {
    console.error("Error generating license:", err.message);
    process.exit(1);
  }
}

module.exports = { generateLicenseToken };
