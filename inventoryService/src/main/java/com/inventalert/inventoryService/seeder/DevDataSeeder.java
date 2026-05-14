package com.inventalert.inventoryService.seeder;

import com.inventalert.inventoryService.kafka.AlertEventProducer;
import com.inventalert.inventoryService.kafka.StockMovementProducer;
import com.inventalert.inventoryService.kafka.TransferEventProducer;
import com.inventalert.inventoryService.model.*;
import com.inventalert.inventoryService.multicompany.CompanyContext;
import com.inventalert.inventoryService.multicompany.CompanySchemaService;
import com.inventalert.inventoryService.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

    @Autowired @Lazy
    private DevDataSeeder self;

    @PersistenceContext
    private EntityManager em;

    private final CompanySchemaService companySchemaService;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final StockLevelRepository stockLevelRepository;
    private final StockMovementRepository stockMovementRepository;
    private final TransferSuggestionRepository transferSuggestionRepository;
    private final RestockAlertRepository restockAlertRepository;
    private final StockMovementProducer movementProducer;
    private final AlertEventProducer alertProducer;
    private final TransferEventProducer transferProducer;

    // Fixed warehouse IDs — must match identityService DevDataSeeder
    private static final String WH_PHARMAPLUS_LAGOS   = "20000000-0000-0000-0000-000000000001";
    private static final String WH_PHARMAPLUS_ABUJA   = "20000000-0000-0000-0000-000000000002";
    private static final String WH_EKOFRESH_LAGOS     = "20000000-0000-0000-0000-000000000003";
    private static final String WH_EKOFRESH_IBADAN    = "20000000-0000-0000-0000-000000000004";
    private static final String WH_LAGOSLIVING_ISLAND = "20000000-0000-0000-0000-000000000005";
    private static final String WH_LAGOSLIVING_LEKKI  = "20000000-0000-0000-0000-000000000006";
    private static final String WH_TECHZONE_IKEJA     = "20000000-0000-0000-0000-000000000007";
    private static final String WH_TECHZONE_ABUJA     = "20000000-0000-0000-0000-000000000008";

    // Tracks the company being seeded so helper methods can publish Kafka events
    private String currentCompanyId;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[Seeder] Starting inventory seed for 4 companies...");
        self.seedCompany("10000000-0000-0000-0000-000000000001", this::seedPharmaplus);
        self.seedCompany("10000000-0000-0000-0000-000000000002", this::seedEkoFresh);
        self.seedCompany("10000000-0000-0000-0000-000000000003", this::seedLagosLiving);
        self.seedCompany("10000000-0000-0000-0000-000000000004", this::seedTechZone);
        log.info("[Seeder] Inventory seed complete");
    }

    public void seedCompany(String companyId, Runnable seeder) {
        currentCompanyId = companyId;
        companySchemaService.provisionSchema(companyId);
        CompanyContext.set(companyId);
        try {
            self.doSeedCompany(seeder, companyId);
        } finally {
            CompanyContext.clear();
            currentCompanyId = null;
        }
    }

    @Transactional
    public void doSeedCompany(Runnable seeder, String companyId) {
        if (warehouseRepository.count() > 0) {
            log.info("[Seeder] Company {} already has inventory data — skipping", companyId);
            return;
        }
        seeder.run();
    }

    // ─── Pharmaplus Nigeria Ltd ───────────────────────────────────────────────

    private void seedPharmaplus() {
        log.info("[Seeder] Seeding Pharmaplus Nigeria Ltd...");
        Warehouse lagos = warehouse(WH_PHARMAPLUS_LAGOS,
                "Pharmaplus Lagos Central",
                "10 Apapa Road, Lagos Island, Lagos", "6.4531", "3.3958");
        Warehouse abuja = warehouse(WH_PHARMAPLUS_ABUJA,
                "Pharmaplus Abuja Hub",
                "Plot 22 Wuse Zone 5, Abuja FCT", "9.0679", "7.4951");

        Product paracetamol = product("Paracetamol 500mg Tablets",        "PHARM-001", "Boxes",   50);
        Product amoxicillin = product("Amoxicillin 250mg Capsules",        "PHARM-002", "Boxes",   30);
        Product vitaminC    = product("Vitamin C 1000mg Tablets",          "PHARM-003", "Bottles", 20);
        Product ibuprofen   = product("Ibuprofen 400mg Tablets",           "PHARM-004", "Boxes",   30);
        Product metformin   = product("Metformin 500mg Tablets",           "PHARM-005", "Boxes",   20);
        Product cipro       = product("Ciprofloxacin 500mg Tablets",       "PHARM-006", "Boxes",   15);
        Product chloroquine = product("Chloroquine Phosphate 250mg",       "PHARM-007", "Boxes",   10);
        Product zinc        = product("Zinc Sulfate 20mg Tablets",         "PHARM-008", "Bottles", 15);
        Product ors         = product("ORS Sachets (Pack of 10)",          "PHARM-009", "Packs",   25);
        Product bpMonitor   = product("Blood Pressure Monitor (Digital)",  "PHARM-010", "Units",    5);
        Product syringes    = product("Insulin Syringes Box/100",          "PHARM-011", "Boxes",   10);
        Product gloves      = product("Surgical Gloves Box/100",           "PHARM-012", "Boxes",   20);
        Product masks       = product("Surgical Face Masks Pack/50",       "PHARM-013", "Packs",   30);
        Product antiseptic  = product("Antiseptic Solution 500ml",         "PHARM-014", "Bottles", 20);
        Product dettol      = product("Dettol Antiseptic Liquid 500ml",    "PHARM-015", "Bottles", 20);
        Product multivit    = product("Multivitamin Capsules Bottle/60",   "PHARM-016", "Bottles", 15);
        Product folicAcid   = product("Folic Acid 5mg Tablets",            "PHARM-017", "Boxes",   10);

        sl(paracetamol, lagos, 200, 50);  sl(paracetamol, abuja,  45, 50);
        sl(amoxicillin, lagos, 120, 30);  sl(amoxicillin, abuja,  25, 30);
        sl(vitaminC,    lagos,  80, 20);  sl(vitaminC,    abuja,  60, 20);
        sl(ibuprofen,   lagos, 150, 30);  sl(ibuprofen,   abuja,  20, 30);
        sl(metformin,   lagos,  90, 20);  sl(metformin,   abuja,  55, 20);
        sl(cipro,       lagos,  60, 15);  sl(cipro,       abuja,  40, 15);
        sl(chloroquine, lagos,  45, 10);  sl(chloroquine, abuja,   8, 10);
        sl(zinc,        lagos,  70, 15);  sl(zinc,        abuja,  30, 15);
        sl(ors,         lagos, 100, 25);  sl(ors,         abuja,  50, 25);
        sl(bpMonitor,   lagos,  20,  5);  sl(bpMonitor,   abuja,  12,  5);
        sl(syringes,    lagos,  50, 10);  sl(syringes,    abuja,   8, 10);
        sl(gloves,      lagos,  80, 20);  sl(gloves,      abuja,  35, 20);
        sl(masks,       lagos, 120, 30);  sl(masks,       abuja,  70, 30);
        sl(antiseptic,  lagos,  60, 20);  sl(antiseptic,  abuja,  45, 20);
        sl(dettol,      lagos,  75, 20);  sl(dettol,      abuja,  50, 20);
        sl(multivit,    lagos,  55, 15);  sl(multivit,    abuja,  40, 15);
        sl(folicAcid,   lagos,  35, 10);  sl(folicAcid,   abuja,  20, 10);

        mv(paracetamol, lagos, MovementType.INTAKE,        100, "PO-PHARM-001");
        mv(amoxicillin, lagos, MovementType.INTAKE,         50, "PO-PHARM-001");
        mv(gloves,      lagos, MovementType.INTAKE,         40, "PO-PHARM-002");
        mv(masks,       lagos, MovementType.INTAKE,         60, "PO-PHARM-002");
        mv(vitaminC,    lagos, MovementType.OUTBOUND_SALE,  20, null);
        mv(ibuprofen,   abuja, MovementType.OUTBOUND_SALE,  10, null);

        tf(paracetamol, lagos, abuja,  80, "533.00", DistanceSource.GOOGLE_MAPS);
        tf(ibuprofen,   lagos, abuja,  60, "533.00", DistanceSource.GOOGLE_MAPS);

        alert(paracetamol, abuja,  45, 50);
        alert(ibuprofen,   abuja,  20, 30);
        alert(chloroquine, abuja,   8, 10);
        alert(syringes,    abuja,   8, 10);

        log.info("[Seeder] Pharmaplus Nigeria Ltd seeded — 17 products, 4 alerts, 2 transfers");
    }

    // ─── Eko Fresh Market ────────────────────────────────────────────────────

    private void seedEkoFresh() {
        log.info("[Seeder] Seeding Eko Fresh Market...");
        Warehouse lagos  = warehouse(WH_EKOFRESH_LAGOS,
                "Eko Fresh Lagos Main",
                "Km 15 Ikorodu Road, Ketu, Lagos", "6.4698", "3.5852");
        Warehouse ibadan = warehouse(WH_EKOFRESH_IBADAN,
                "Eko Fresh Ibadan Depot",
                "12 Ring Road, Ibadan, Oyo State", "7.3775", "3.9470");

        Product rice      = product("Rice 50kg Bag",                     "EKO-001", "Bags",    15);
        Product beans     = product("Beans (Black-eye) 50kg Bag",        "EKO-002", "Bags",    15);
        Product palmOil   = product("Palm Oil 25L Jerry Can",            "EKO-003", "Cans",    10);
        Product tomPaste  = product("Tomato Paste Crate/24",             "EKO-004", "Crates",  10);
        Product semolina  = product("Semolina 10kg Bag",                 "EKO-005", "Bags",    20);
        Product garri     = product("Garri (White) 25kg Bag",            "EKO-006", "Bags",    20);
        Product indomie   = product("Indomie Instant Noodles Carton/40", "EKO-007", "Cartons", 25);
        Product milo      = product("Milo 500g Tin",                     "EKO-008", "Tins",    30);
        Product sugar     = product("Sugar 50kg Bag",                    "EKO-009", "Bags",    10);
        Product soyaOil   = product("Soya Oil 5L Bottle",                "EKO-010", "Bottles", 15);
        Product milk      = product("Peak Evaporated Milk Carton/24",    "EKO-011", "Cartons", 12);
        Product flour     = product("Flour 50kg Bag",                    "EKO-012", "Bags",    10);
        Product salt      = product("Iodised Salt 50kg Bag",             "EKO-013", "Bags",    10);
        Product groundnut = product("Groundnut Oil 5L Bottle",           "EKO-014", "Bottles", 15);
        Product yamFlour  = product("Yam Flour (Elubo) 10kg",            "EKO-015", "Bags",    15);
        Product cornmeal  = product("Cornmeal (Ogi) 10kg Bag",           "EKO-016", "Bags",    10);

        sl(rice,      lagos,  80, 15);  sl(rice,      ibadan,  5, 15);
        sl(beans,     lagos,  60, 15);  sl(beans,     ibadan,  8, 15);
        sl(palmOil,   lagos,  40, 10);  sl(palmOil,   ibadan,  3, 10);
        sl(tomPaste,  lagos,  50, 10);  sl(tomPaste,  ibadan, 20, 10);
        sl(semolina,  lagos,  90, 20);  sl(semolina,  ibadan, 45, 20);
        sl(garri,     lagos,  70, 20);  sl(garri,     ibadan, 30, 20);
        sl(indomie,   lagos, 100, 25);  sl(indomie,   ibadan, 55, 25);
        sl(milo,      lagos, 120, 30);  sl(milo,      ibadan, 60, 30);
        sl(sugar,     lagos,  45, 10);  sl(sugar,     ibadan, 18, 10);
        sl(soyaOil,   lagos,  60, 15);  sl(soyaOil,   ibadan, 25, 15);
        sl(milk,      lagos,  48, 12);  sl(milk,      ibadan, 30, 12);
        sl(flour,     lagos,  55, 10);  sl(flour,     ibadan, 22, 10);
        sl(salt,      lagos,  40, 10);  sl(salt,      ibadan, 15, 10);
        sl(groundnut, lagos,  55, 15);  sl(groundnut, ibadan, 28, 15);
        sl(yamFlour,  lagos,  60, 15);  sl(yamFlour,  ibadan, 35, 15);
        sl(cornmeal,  lagos,  42, 10);  sl(cornmeal,  ibadan, 25, 10);

        mv(rice,    lagos,  MovementType.INTAKE,        50, "PO-EKO-001");
        mv(beans,   lagos,  MovementType.INTAKE,        30, "PO-EKO-001");
        mv(indomie, lagos,  MovementType.INTAKE,        60, "PO-EKO-002");
        mv(milo,    lagos,  MovementType.INTAKE,        40, "PO-EKO-002");
        mv(garri,   ibadan, MovementType.OUTBOUND_SALE, 15, null);
        mv(sugar,   ibadan, MovementType.OUTBOUND_SALE, 10, null);

        tf(rice,    lagos, ibadan, 30, "128.00", DistanceSource.GOOGLE_MAPS);
        tf(beans,   lagos, ibadan, 25, "128.00", DistanceSource.GOOGLE_MAPS);
        tf(palmOil, lagos, ibadan, 15, "128.00", DistanceSource.GOOGLE_MAPS);

        alert(rice,    ibadan,  5, 15);
        alert(beans,   ibadan,  8, 15);
        alert(palmOil, ibadan,  3, 10);

        log.info("[Seeder] Eko Fresh Market seeded — 16 products, 3 alerts, 3 transfers");
    }

    // ─── Lagos Living Furniture ───────────────────────────────────────────────

    private void seedLagosLiving() {
        log.info("[Seeder] Seeding Lagos Living Furniture...");
        Warehouse island = warehouse(WH_LAGOSLIVING_ISLAND,
                "Lagos Living Island Showroom",
                "45 Broad Street, Lagos Island, Lagos", "6.4536", "3.3966");
        Warehouse lekki  = warehouse(WH_LAGOSLIVING_LEKKI,
                "Lagos Living Lekki Distribution",
                "Km 21 Lekki-Epe Expressway, Lekki, Lagos", "6.4281", "3.5418");

        Product sofa3      = product("3-Seater Sofa (Fabric)",          "FURN-001", "Units", 3);
        Product execChair  = product("Executive Leather Chair",          "FURN-002", "Units", 3);
        Product diningTbl  = product("Dining Table 6-Seater",           "FURN-003", "Units", 2);
        Product bedFrame   = product("Double Bed Frame (Mahogany)",      "FURN-004", "Units", 3);
        Product wardrobe   = product("Wardrobe 3-Door Sliding",         "FURN-005", "Units", 3);
        Product tvStand    = product("TV Stand with Shelves",            "FURN-006", "Units", 4);
        Product bookshelf  = product("Bookshelf 5-Tier (Wood)",         "FURN-007", "Units", 4);
        Product desk       = product("Study Desk with Drawer",          "FURN-008", "Units", 3);
        Product bedside    = product("Bedside Table (Pair)",             "FURN-009", "Units", 4);
        Product cofTbl     = product("Center Coffee Table (Glass Top)", "FURN-010", "Units", 3);
        Product kitchen    = product("Kitchen Cabinet 2-Door",          "FURN-011", "Units", 3);
        Product mirror     = product("Dressing Mirror (Standing)",       "FURN-012", "Units", 4);
        Product offChair   = product("Office Chair (Mesh Back)",        "FURN-013", "Units", 4);
        Product cornerSofa = product("L-Shaped Corner Sofa",            "FURN-014", "Units", 2);
        Product bunkBed    = product("Bunk Bed (Metal Frame)",          "FURN-015", "Units", 3);

        sl(sofa3,      island, 15, 3);  sl(sofa3,      lekki,  2, 3);
        sl(execChair,  island, 12, 3);  sl(execChair,  lekki,  8, 3);
        sl(diningTbl,  island,  8, 2);  sl(diningTbl,  lekki,  5, 2);
        sl(bedFrame,   island, 18, 3);  sl(bedFrame,   lekki, 10, 3);
        sl(wardrobe,   island, 20, 3);  sl(wardrobe,   lekki,  1, 3);
        sl(tvStand,    island, 25, 4);  sl(tvStand,    lekki, 14, 4);
        sl(bookshelf,  island, 20, 4);  sl(bookshelf,  lekki, 12, 4);
        sl(desk,       island, 15, 3);  sl(desk,       lekki,  8, 3);
        sl(bedside,    island, 22, 4);  sl(bedside,    lekki, 16, 4);
        sl(cofTbl,     island, 18, 3);  sl(cofTbl,     lekki,  9, 3);
        sl(kitchen,    island, 12, 3);  sl(kitchen,    lekki,  6, 3);
        sl(mirror,     island, 20, 4);  sl(mirror,     lekki, 11, 4);
        sl(offChair,   island, 30, 4);  sl(offChair,   lekki, 18, 4);
        sl(cornerSofa, island,  8, 2);  sl(cornerSofa, lekki,  3, 2);
        sl(bunkBed,    island, 14, 3);  sl(bunkBed,    lekki,  7, 3);

        mv(sofa3,     island, MovementType.INTAKE,         5, "PO-FURN-001");
        mv(bedFrame,  island, MovementType.INTAKE,         8, "PO-FURN-001");
        mv(wardrobe,  island, MovementType.INTAKE,         6, "PO-FURN-002");
        mv(offChair,  island, MovementType.INTAKE,        10, "PO-FURN-002");
        mv(execChair, lekki,  MovementType.OUTBOUND_SALE,  2, null);
        mv(diningTbl, lekki,  MovementType.OUTBOUND_SALE,  1, null);

        tf(sofa3,    island, lekki,  5, "19.00", DistanceSource.HAVERSINE);
        tf(wardrobe, island, lekki,  6, "19.00", DistanceSource.HAVERSINE);

        alert(sofa3,    lekki,  2, 3);
        alert(wardrobe, lekki,  1, 3);

        log.info("[Seeder] Lagos Living Furniture seeded — 15 products, 2 alerts, 2 transfers");
    }

    // ─── TechZone Gadgets ─────────────────────────────────────────────────────

    private void seedTechZone() {
        log.info("[Seeder] Seeding TechZone Gadgets...");
        Warehouse ikeja = warehouse(WH_TECHZONE_IKEJA,
                "TechZone Ikeja Computer Village",
                "Computer Village, Obafemi Awolowo Way, Ikeja, Lagos", "6.5954", "3.3434");
        Warehouse abuja = warehouse(WH_TECHZONE_ABUJA,
                "TechZone Abuja Annex",
                "Plot 44 Garki Area 11, Abuja FCT", "9.0165", "7.4892");

        Product galaxyA55  = product("Samsung Galaxy A55 128GB",          "TECH-001", "Units",  5);
        Product iphone15   = product("Apple iPhone 15 256GB",             "TECH-002", "Units",  5);
        Product camon30    = product("Tecno Camon 30 128GB",              "TECH-003", "Units",  8);
        Product redmiNote  = product("Xiaomi Redmi Note 13 256GB",        "TECH-004", "Units",  5);
        Product airpods    = product("Apple AirPods Pro (2nd Gen)",       "TECH-005", "Units",  5);
        Product galaxyBuds = product("Samsung Galaxy Buds2 Pro",         "TECH-006", "Units",  5);
        Product charger    = product("Anker 65W USB-C Fast Charger",      "TECH-007", "Units", 15);
        Product powerBank  = product("Romoss Power Bank 20000mAh",        "TECH-008", "Units", 10);
        Product screenProt = product("Tempered Glass Screen Protector",   "TECH-009", "Units", 20);
        Product phoneCase  = product("iPhone 15 Silicone Case",           "TECH-010", "Units", 20);
        Product hdmi       = product("HDMI Cable 2m (4K)",                "TECH-011", "Units", 15);
        Product coolingPad = product("Laptop Cooling Pad",                "TECH-012", "Units", 10);
        Product mouse      = product("Logitech M185 Wireless Mouse",      "TECH-013", "Units", 15);
        Product speaker    = product("JBL Flip 6 Bluetooth Speaker",      "TECH-014", "Units",  8);
        Product watch6     = product("Samsung Galaxy Watch6 44mm",        "TECH-015", "Units",  5);
        Product usbHub     = product("USB Hub 7-Port (USB-C)",            "TECH-016", "Units", 10);
        Product keyboard   = product("Rapoo E9350G Wireless Keyboard",    "TECH-017", "Units", 10);

        sl(galaxyA55,  ikeja,  25,  5);  sl(galaxyA55,  abuja,  4,  5);
        sl(iphone15,   ikeja,  30,  5);  sl(iphone15,   abuja,  3,  5);
        sl(camon30,    ikeja,  40,  8);  sl(camon30,    abuja, 12,  8);
        sl(redmiNote,  ikeja,  22,  5);  sl(redmiNote,  abuja,  8,  5);
        sl(airpods,    ikeja,  20,  5);  sl(airpods,    abuja, 10,  5);
        sl(galaxyBuds, ikeja,  18,  5);  sl(galaxyBuds, abuja,  7,  5);
        sl(charger,    ikeja,  80, 15);  sl(charger,    abuja, 35, 15);
        sl(powerBank,  ikeja,  60, 10);  sl(powerBank,  abuja, 20, 10);
        sl(screenProt, ikeja, 120, 20);  sl(screenProt, abuja, 55, 20);
        sl(phoneCase,  ikeja, 100, 20);  sl(phoneCase,  abuja, 40, 20);
        sl(hdmi,       ikeja,  75, 15);  sl(hdmi,       abuja, 28, 15);
        sl(coolingPad, ikeja,  45, 10);  sl(coolingPad, abuja, 18, 10);
        sl(mouse,      ikeja,  70, 15);  sl(mouse,      abuja, 30, 15);
        sl(speaker,    ikeja,  35,  8);  sl(speaker,    abuja, 12,  8);
        sl(watch6,     ikeja,  20,  5);  sl(watch6,     abuja,  6,  5);
        sl(usbHub,     ikeja,  50, 10);  sl(usbHub,     abuja, 22, 10);
        sl(keyboard,   ikeja,  55, 10);  sl(keyboard,   abuja, 25, 10);

        mv(galaxyA55,  ikeja, MovementType.INTAKE,        15, "PO-TECH-001");
        mv(iphone15,   ikeja, MovementType.INTAKE,        10, "PO-TECH-001");
        mv(charger,    ikeja, MovementType.INTAKE,        50, "PO-TECH-002");
        mv(powerBank,  ikeja, MovementType.INTAKE,        30, "PO-TECH-002");
        mv(camon30,    abuja, MovementType.OUTBOUND_SALE,  5, null);
        mv(screenProt, abuja, MovementType.OUTBOUND_SALE, 10, null);

        tf(galaxyA55, ikeja, abuja, 10, "533.00", DistanceSource.GOOGLE_MAPS);
        tf(iphone15,  ikeja, abuja,  8, "533.00", DistanceSource.GOOGLE_MAPS);

        alert(galaxyA55, abuja, 4, 5);
        alert(iphone15,  abuja, 3, 5);

        log.info("[Seeder] TechZone Gadgets seeded — 17 products, 2 alerts, 2 transfers");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Warehouse warehouse(String id, String name, String address, String lat, String lon) {
        return warehouseRepository.findById(id).orElseGet(() -> {
            // em.persist() fails with @GeneratedValue + manual ID in Hibernate 7 (treats it as detached),
            // and merge() fails when the row doesn't exist yet. Use a native INSERT to bypass both.
            em.createNativeQuery(
                "INSERT INTO warehouses (id, name, address, latitude, longitude, isActive, createdBy, createdAt, updatedAt) " +
                "VALUES (?, ?, ?, ?, ?, 1, 'seeder', NOW(), NOW())"
            )
            .setParameter(1, id)
            .setParameter(2, name)
            .setParameter(3, address)
            .setParameter(4, new BigDecimal(lat))
            .setParameter(5, new BigDecimal(lon))
            .executeUpdate();
            em.flush();
            return warehouseRepository.findById(id).orElseThrow();
        });
    }

    private Product product(String name, String sku, String unit, int threshold) {
        return productRepository.save(Product.builder()
                .name(name).sku(sku).unitOfMeasure(unit)
                .defaultThreshold(threshold).isActive(true).createdBy("seeder")
                .build());
    }

    private void sl(Product p, Warehouse w, int current, int threshold) {
        stockLevelRepository.save(StockLevel.builder()
                .productId(p.getId()).warehouseId(w.getId())
                .currentStock(current).threshold(threshold)
                .build());
    }

    private void mv(Product p, Warehouse w, MovementType type, int qty, String ref) {
        StockMovement saved = stockMovementRepository.save(StockMovement.builder()
                .productId(p.getId()).warehouseId(w.getId())
                .type(type).quantity(qty).referenceId(ref)
                .createdBy("seeder")
                .build());
        movementProducer.publishMovementCreated(
                currentCompanyId, saved.getId(), p.getId(), w.getId(), type, qty);
    }

    private void tf(Product p, Warehouse from, Warehouse to,
                    int qty, String distKm, DistanceSource src) {
        TransferSuggestion saved = transferSuggestionRepository.save(TransferSuggestion.builder()
                .productId(p.getId())
                .fromWarehouseId(from.getId()).toWarehouseId(to.getId())
                .quantity(qty).distanceKm(new BigDecimal(distKm))
                .distanceSource(src).status(TransferStatus.SUGGESTED)
                .build());
        transferProducer.publishTransferSuggestionCreated(
                currentCompanyId, saved.getId(),
                from.getId(), to.getId(),
                p.getId(), qty, Double.parseDouble(distKm));
    }

    private void alert(Product p, Warehouse w, int stockAtAlert, int threshold) {
        RestockAlert saved = restockAlertRepository.save(RestockAlert.builder()
                .productId(p.getId()).warehouseId(w.getId())
                .stockAtAlert(stockAtAlert).threshold(threshold)
                .status(AlertStatus.OPEN)
                .build());
        alertProducer.publishAlertCreated(
                currentCompanyId, saved.getId(), p.getId(), w.getId(),
                null, stockAtAlert, threshold);
    }
}
