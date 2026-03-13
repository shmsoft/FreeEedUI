from concurrent.futures import ThreadPoolExecutor, as_completed

batches = [["chunk 1"], ["chunk 2"], ["chunk 3"]]
def _analyze(b, i):
    return f"Result of {b[0]} at {i}"

batch_analyses = [None] * len(batches)
with ThreadPoolExecutor(max_workers=4) as pool:
    futures = {pool.submit(_analyze, b, i): i for i, b in enumerate(batches)}
    for f in as_completed(futures):
        batch_analyses[futures[f]] = f.result()

print(batch_analyses)

doc_chunks = {"doc1": ["c1", "c2"], "doc2": ["c3"]}
per_doc = []
def _cnt(s, c):
    return {"source": s, "chunks": c}

with ThreadPoolExecutor(max_workers=4) as pool:
    futures = {pool.submit(_cnt, s, c): s for s, c in doc_chunks.items()}
    for f in as_completed(futures):
        per_doc.append(f.result())
print(per_doc)
