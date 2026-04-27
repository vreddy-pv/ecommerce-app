-- Seed categories
INSERT INTO categories (id, name) VALUES
  (1, 'Electronics'),
  (2, 'Clothing'),
  (3, 'Home & Kitchen'),
  (4, 'Books'),
  (5, 'Sports & Outdoors')
ON CONFLICT (id) DO NOTHING;

SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));

-- Seed products
INSERT INTO products (sku, name, description, price, category_id, is_active) VALUES
  ('ELEC-001', 'Wireless Noise-Cancelling Headphones', 'Premium over-ear headphones with 30-hour battery and active noise cancellation.', 149.99, 1, true),
  ('ELEC-002', 'Mechanical Gaming Keyboard', 'TKL layout, RGB backlit, tactile blue switches, USB-C.', 89.99, 1, true),
  ('ELEC-003', '4K Webcam', '3840x2160 resolution, built-in ring light, plug-and-play USB.', 119.99, 1, true),
  ('ELEC-004', 'Portable Bluetooth Speaker', 'IPX7 waterproof, 360° sound, 12-hour playtime.', 59.99, 1, true),
  ('ELEC-005', 'USB-C Hub 7-in-1', 'HDMI 4K, 3x USB-A, SD card reader, 100W PD charging.', 39.99, 1, true),
  ('CLTH-001', 'Men''s Classic Oxford Shirt', '100% cotton, slim fit, available in blue and white.', 34.99, 2, true),
  ('CLTH-002', 'Women''s Yoga Leggings', 'High-waist, moisture-wicking, 4-way stretch fabric.', 29.99, 2, true),
  ('CLTH-003', 'Unisex Fleece Hoodie', 'Heavyweight 80/20 cotton-poly blend, kangaroo pocket.', 44.99, 2, true),
  ('CLTH-004', 'Men''s Running Shorts', 'Lightweight mesh, 5-inch inseam, built-in liner.', 24.99, 2, true),
  ('CLTH-005', 'Women''s Casual Blazer', 'Tailored fit, single-button, machine washable.', 64.99, 2, true),
  ('HOME-001', 'Stainless Steel Cookware Set', '10-piece set, tri-ply base, dishwasher safe, induction compatible.', 199.99, 3, true),
  ('HOME-002', 'Bamboo Cutting Board Set', 'Set of 3 graduated sizes, juice grooves, non-slip feet.', 27.99, 3, true),
  ('HOME-003', 'Air Purifier HEPA H13', 'Covers 500 sq ft, removes 99.97% particles, ultra-quiet 25dB.', 89.99, 3, true),
  ('HOME-004', 'Pour Over Coffee Set', 'Borosilicate glass dripper, gooseneck kettle, and filters included.', 49.99, 3, true),
  ('HOME-005', 'Ergonomic Office Chair', 'Lumbar support, adjustable armrests, breathable mesh back.', 249.99, 3, true),
  ('BOOK-001', 'Clean Code', 'A Handbook of Agile Software Craftsmanship — Robert C. Martin.', 34.99, 4, true),
  ('BOOK-002', 'Designing Data-Intensive Applications', 'The definitive guide to scalable, reliable distributed systems — Martin Kleppmann.', 49.99, 4, true),
  ('BOOK-003', 'The Pragmatic Programmer', '20th Anniversary Edition — David Thomas & Andrew Hunt.', 39.99, 4, true),
  ('BOOK-004', 'System Design Interview Vol. 2', 'An insider''s guide — Alex Xu & Sahn Lam.', 29.99, 4, true),
  ('BOOK-005', 'Deep Work', 'Rules for focused success in a distracted world — Cal Newport.', 19.99, 4, true),
  ('SPRT-001', 'Adjustable Dumbbell Set', '5-50 lbs per dumbbell, quick-adjust selector dial, compact storage.', 299.99, 5, true),
  ('SPRT-002', 'Foam Roller Extra Firm', '36-inch, high-density EVA foam, for deep tissue massage.', 24.99, 5, true),
  ('SPRT-003', 'Hydration Running Vest', '10L capacity, 2L bladder included, reflective strips.', 74.99, 5, true),
  ('SPRT-004', 'Resistance Bands Set', 'Set of 5 levels 10-50 lbs, latex-free, includes handles and door anchor.', 19.99, 5, true),
  ('SPRT-005', 'Camping Headlamp 1000 Lumen', 'Rechargeable USB-C, 5 modes, IPX6 waterproof, red night-vision mode.', 34.99, 5, true)
ON CONFLICT (sku) DO NOTHING;
