import { PageMeta } from '../models/pagination.model';


export function toApiPage(uiPage: number): number {
  return uiPage - 1;
}


export function fromApiPage(apiPage: number): number {
  return apiPage + 1;
}

export function computeDisplayRange(meta: PageMeta): { start: number; end: number } {
  if (!meta || meta.total === 0) {
    return { start: 0, end: 0 };
  }
  const start = (meta.page - 1) * meta.pageSize + 1;
  const end = Math.min(meta.page * meta.pageSize, meta.total);
  return { start, end };
}
