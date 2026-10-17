function base64urlToBuffer(base64url: string): ArrayBuffer {
  const padded = base64url.replace(/-/g, "+").replace(/_/g, "/");
  const padding = "=".repeat((4 - (padded.length % 4)) % 4);
  const binary = atob(padded + padding);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes.buffer;
}

function bufferToBase64url(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

export interface RegistrationOptionsResponse {
  rp: { id: string; name: string };
  user: { id: string; name: string; displayName: string };
  challenge: string;
  pubKeyCredParams: { type: "public-key"; alg: number }[];
  timeout: number;
  attestation: AttestationConveyancePreference;
}

export interface LoginOptionsResponse {
  challenge: string;
  timeout: number;
  rpId: string;
  userVerification: UserVerificationRequirement;
  allowCredentials: { type: "public-key"; id: string }[];
}

export async function createCredential(options: RegistrationOptionsResponse) {
  const publicKey: CredentialCreationOptions["publicKey"] = {
    ...options,
    challenge: base64urlToBuffer(options.challenge),
    user: {
      ...options.user,
      id: base64urlToBuffer(options.user.id),
    },
  };

  const credential = (await navigator.credentials.create({ publicKey })) as PublicKeyCredential;
  const response = credential.response as AuthenticatorAttestationResponse;

  return {
    credentialId: credential.id,
    attestationObject: bufferToBase64url(response.attestationObject),
    clientDataJSON: bufferToBase64url(response.clientDataJSON),
    transports: response.getTransports?.() ?? [],
  };
}

export async function getCredential(options: LoginOptionsResponse) {
  const publicKey: CredentialRequestOptions["publicKey"] = {
    ...options,
    challenge: base64urlToBuffer(options.challenge),
    allowCredentials: options.allowCredentials.map((cred) => ({
      type: cred.type,
      id: base64urlToBuffer(cred.id),
    })),
  };

  const assertion = (await navigator.credentials.get({ publicKey })) as PublicKeyCredential;
  const response = assertion.response as AuthenticatorAssertionResponse;

  return {
    credentialId: assertion.id,
    authenticatorData: bufferToBase64url(response.authenticatorData),
    clientDataJSON: bufferToBase64url(response.clientDataJSON),
    signature: bufferToBase64url(response.signature),
    userHandle: response.userHandle ? bufferToBase64url(response.userHandle) : undefined,
  };
}

export function isWebAuthnSupported() {
  return typeof window !== "undefined" && !!window.PublicKeyCredential;
}
