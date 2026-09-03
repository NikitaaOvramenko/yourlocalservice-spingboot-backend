-- The four live sites and the exact service options their quote forms offer.
--
-- Service names are transcribed verbatim from each form's "Select Service Type" /
-- "What you need" / "What do you need done?" control, so that what a client picks on
-- the site is what lands in quote_service. Change a name here only alongside the form.
--
-- description is left NULL: the forms carry their own copy, and inventing marketing
-- text here would just be wrong in a second place.

INSERT INTO organization (id, name, slug, contact_email, active) VALUES
    (1, 'YourLocalPaints',       'yourlocalpaints',       'info@yourlocalservice.co', TRUE),
    (2, 'YourLocalHandyman',     'yourlocalhandyman',     'info@yourlocalservice.co', TRUE),
    (3, 'TCS',                   'tcs',                   'tcs.ontario@gmail.com',    TRUE),
    (4, 'YourLocalJunkRemoval',  'yourlocaljunkremoval',  'info@yourlocalservice.co', TRUE);

INSERT INTO service (id, name, slug, active) VALUES
    -- YourLocalPaints
    (1,  'Interior Painting',             'interior-painting',             TRUE),
    (2,  'Exterior Painting',             'exterior-painting',             TRUE),
    (3,  'Deck & fence staining/painting','deck-fence-staining-painting',  TRUE),
    (4,  'Cabinet painting / refinishing','cabinet-painting-refinishing',  TRUE),

    -- YourLocalHandyman
    (5,  'General Repairs',               'general-repairs',               TRUE),
    (6,  'Appliance Repair',              'appliance-repair',              TRUE),
    (7,  'Junk Removal',                  'junk-removal',                  TRUE),
    (8,  'Carpentry & Furniture Assembly','carpentry-furniture-assembly',  TRUE),
    (9,  'Minor Plumbing Fixes',          'minor-plumbing-fixes',          TRUE),
    (10, 'Drywall & Wall Patching',       'drywall-wall-patching',         TRUE),

    -- TCS
    (11, 'Repair and insulation of roofs','repair-and-insulation-of-roofs',TRUE),
    (12, 'Thermal imaging survey',        'thermal-imaging-survey',        TRUE),
    (13, 'Waterproofing',                 'waterproofing',                 TRUE),
    (14, 'Civil works',                   'civil-works',                   TRUE),
    (15, 'Dismantling work',              'dismantling-work',              TRUE),
    (16, 'Landscaping and interlocking',  'landscaping-and-interlocking',  TRUE),
    (17, 'Deck & Fences',                 'deck-and-fences',               TRUE),
    (18, 'Retaining Walls',               'retaining-walls',               TRUE),
    (19, 'Gazebo',                        'gazebo',                        TRUE),
    (20, 'Facade and finishing works',    'facade-and-finishing-works',    TRUE),

    -- YourLocalJunkRemoval
    (21, 'Furniture Removal',             'furniture-removal',             TRUE),
    (22, 'Property Cleanouts',            'property-cleanouts',            TRUE),
    (23, 'Appliance Removal',             'appliance-removal',             TRUE),
    (24, 'Waste Removal',                 'waste-removal',                 TRUE);

-- Which org offers what. Note the deliberate near-collisions kept as separate rows,
-- because they are genuinely different jobs sold by different businesses:
--   'Appliance Repair'  (Handyman)  vs 'Appliance Removal' (JunkRemoval)
--   'Junk Removal'      (Handyman)  vs the YourLocalJunkRemoval org's own catalogue
--   'Deck & fence staining/painting' (Paints) vs 'Deck & Fences' (TCS)
INSERT INTO organization_service (organization_id, service_id) VALUES
    (1, 1), (1, 2), (1, 3), (1, 4),
    (2, 5), (2, 6), (2, 7), (2, 8), (2, 9), (2, 10),
    (3, 11), (3, 12), (3, 13), (3, 14), (3, 15),
    (3, 16), (3, 17), (3, 18), (3, 19), (3, 20),
    (4, 21), (4, 22), (4, 23), (4, 24);

-- Mandatory: the identity sequences know nothing about the explicit ids inserted
-- above, so without this the first row the application inserts would reuse id 1 and
-- violate the primary key.
SELECT setval(pg_get_serial_sequence('organization', 'id'),
              (SELECT COALESCE(MAX(id), 1) FROM organization));
SELECT setval(pg_get_serial_sequence('service', 'id'),
              (SELECT COALESCE(MAX(id), 1) FROM service));
