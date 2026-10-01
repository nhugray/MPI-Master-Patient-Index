export function maskPhoneNumber(phone: string | null | undefined): string {
  if (!phone) return '-';
  if (phone.length <= 6) return phone;
  return phone.substring(0, 3) + '***' + phone.substring(phone.length - 3);
}

export function maskNationalId(nationalId: string | null | undefined): string {
  if (!nationalId) return '-';
  if (nationalId.length <= 4) return nationalId;
  return nationalId.substring(0, 3) + '***' + nationalId.substring(nationalId.length - 3);
}

export function maskHealthInsurance(insurance: string | null | undefined): string {
  if (!insurance) return '-';
  if (insurance.length <= 6) return insurance;
  return insurance.substring(0, 4) + '***' + insurance.substring(insurance.length - 4);
}

export function getInitials(fullName: string | null | undefined): string {
  if (!fullName) return '?';
  const parts = fullName.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  if (parts.length === 1) {
    return parts[0].substring(0, 2).toUpperCase();
  }
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

export function truncate(text: string | null | undefined, length = 50): string {
  if (!text) return '-';
  return text.length > length ? text.substring(0, length) + '...' : text;
}

export function normalizeString(value: string | null | undefined): string {
  return (value || '').trim().toLowerCase();
}

export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}
