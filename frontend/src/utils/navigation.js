export function pageUrl(path, page) {
  const [base, query] = path.split("?");
  const q = new URLSearchParams(query);
  q.set("page", page);
  return base + "?" + q;
}
