const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const CUSTOMER_UID = "customer_123";
const SELLER_UID = "seller_456";
const OTHER_UID = "other_789";
const ADMIN_UID = "admin_001";
const ADMIN_EMAIL = "ansariproducts05@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated users: denied on all collections", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("products").where("approvalStatus", "==", "APPROVED").get());
  await assertFails(db.collection("categories").get());
  await assertFails(db.collection("orders").where("customerId", "==", CUSTOMER_UID).get());
  await assertFails(db.collection("sellers").doc(SELLER_UID).get());
});

test("Email spoofing test: unverified admin email is rejected", async () => {
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  const ts = pastDate();
  await assertFails(
    spoofDb.collection("categories").doc("cat_1").set({
      name: "Fake",
      iconKey: "clothes",
      accentHex: 16711680,
      sortOrder: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Verified Admin: can manage categories, banners, payment_accounts, and list customers", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  const ts = pastDate();
  await assertSucceeds(
    adminDb.collection("categories").doc("cat_clothes").set({
      name: "Clothes",
      iconKey: "clothes",
      accentHex: 42577,
      sortOrder: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    adminDb.collection("payment_accounts").doc("acc_easypaisa").set({
      providerName: "Easypaisa",
      accountNumber: "03455368275",
      accountTitle: "ZM Bazaar",
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(adminDb.collection("users").get());
});

test("Users: profile creation, self-escalation rejection, and PII isolation", async () => {
  const custDb = testEnv.authenticatedContext(CUSTOMER_UID, { email: "cust@example.com", email_verified: true }).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID, { email: "other@example.com", email_verified: true }).firestore();
  const ts = pastDate();

  // Reject role escalation on create
  await assertFails(
    custDb.collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      fullName: "Bilal",
      phone: "03001112233",
      email: "cust@example.com",
      city: "Mirpur",
      role: "ADMIN",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject @DocumentId 'id' ghost field
  await assertFails(
    custDb.collection("users").doc(CUSTOMER_UID).set({
      id: CUSTOMER_UID,
      userId: CUSTOMER_UID,
      fullName: "Bilal",
      phone: "03001112233",
      email: "cust@example.com",
      city: "Mirpur",
      role: "CUSTOMER",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid customer profile create
  await assertSucceeds(
    custDb.collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      fullName: "Bilal Ahmed",
      phone: "03001112233",
      email: "cust@example.com",
      city: "Mirpur",
      role: "CUSTOMER",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Other user cannot read Bilal's PII or list /users
  await assertFails(otherDb.collection("users").doc(CUSTOMER_UID).get());
  await assertFails(otherDb.collection("users").get());

  // Customer cannot change role on update, but can update city/phone
  await assertFails(
    custDb.collection("users").doc(CUSTOMER_UID).update({
      role: "ADMIN",
      updatedAt: pastDate(500),
    })
  );
  await assertSucceeds(
    custDb.collection("users").doc(CUSTOMER_UID).update({
      city: "Barnala",
      updatedAt: pastDate(500),
    })
  );
});

test("Sellers: PII privacy from customers, seller account updates, admin approval", async () => {
  const sellerDb = testEnv.authenticatedContext(SELLER_UID, { email: "seller@example.com", email_verified: true }).firestore();
  const custDb = testEnv.authenticatedContext(CUSTOMER_UID, { email: "cust@example.com", email_verified: true }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const ts = pastDate();

  await assertSucceeds(
    sellerDb.collection("sellers").doc(SELLER_UID).set({
      userId: SELLER_UID,
      shopName: "Kashmir Craft",
      ownerName: "Hamza",
      phone: "03451234567",
      cnic: "81101-1234567-1",
      paymentMethod: "Easypaisa",
      paymentAccountNumber: "03451234567",
      paymentAccountTitle: "Hamza",
      shopAddress: "Barnala AJK",
      status: "PENDING",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Customer cannot read seller's private payout account or CNIC
  await assertFails(custDb.collection("sellers").doc(SELLER_UID).get());
  await assertFails(custDb.collection("sellers").get());

  // Seller cannot self-approve
  await assertFails(
    sellerDb.collection("sellers").doc(SELLER_UID).update({
      status: "APPROVED",
      updatedAt: pastDate(500),
    })
  );

  // Seller CAN change own payment account
  await assertSucceeds(
    sellerDb.collection("sellers").doc(SELLER_UID).update({
      paymentMethod: "JazzCash",
      paymentAccountNumber: "03009998877",
      updatedAt: pastDate(500),
    })
  );

  // Admin approves seller
  await assertSucceeds(
    adminDb.collection("sellers").doc(SELLER_UID).update({
      status: "APPROVED",
      updatedAt: pastDate(300),
    })
  );
});

test("Products: filtered approved query succeeds; unfiltered query fails", async () => {
  const sellerDb = testEnv.authenticatedContext(SELLER_UID).firestore();
  const custDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const ts = pastDate();

  await assertSucceeds(
    sellerDb.collection("products").doc("prod_1").set({
      sellerUid: SELLER_UID,
      shopName: "Kashmir Craft",
      name: "Custom Name Mug",
      description: "Ceramic mug",
      category: "Custom Name Products",
      price: 850,
      oldPrice: 1200,
      stock: 20,
      rating: 4.9,
      reviewCount: 10,
      imageUrls: "preset:custom_mug",
      flashSale: true,
      featured: true,
      allowsCustomName: true,
      approvalStatus: "PENDING",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Customer cannot view PENDING product or run unfiltered list
  await assertFails(custDb.collection("products").doc("prod_1").get());
  await assertFails(custDb.collection("products").get());

  // Admin approves product
  await assertSucceeds(
    adminDb.collection("products").doc("prod_1").update({
      approvalStatus: "APPROVED",
      updatedAt: pastDate(400),
    })
  );

  // Customer filtered query succeeds
  await assertSucceeds(
    custDb.collection("products").where("approvalStatus", "==", "APPROVED").get()
  );
});

test("Orders: multi-vendor isolation, seller status update, 10% commission & terminal lock", async () => {
  const custDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const sellerDb = testEnv.authenticatedContext(SELLER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const ts = pastDate();

  await assertSucceeds(
    custDb.collection("orders").doc("ord_1").set({
      orderCode: "ZMB-2026-1001",
      customerId: CUSTOMER_UID,
      customerName: "Bilal Ahmed",
      phone: "03001112233",
      address: "Street 4, Barnala",
      city: "Barnala",
      note: "Call before delivery",
      paymentMethod: "Easypaisa",
      paymentAccountInfo: "Easypaisa: 03455368275",
      paymentScreenshotUri: "",
      subtotal: 2000,
      deliveryCharges: 0,
      grandTotal: 2000,
      commissionAmount: 0,
      sellerEarningAmount: 0,
      payoutStatus: "NONE",
      status: "Pending",
      sellerUids: [SELLER_UID],
      items: [
        {
          productId: "prod_1",
          sellerUid: SELLER_UID,
          shopName: "Kashmir Craft",
          productName: "Custom Mug",
          imageUri: "preset:custom_mug",
          unitPrice: 2000,
          quantity: 1,
          lineTotal: 2000,
          commissionAmount: 0,
          sellerEarningAmount: 0,
          payoutStatus: "NONE",
          customName: "Mujtaba",
          customPhotoUri: "",
        },
      ],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Unrelated user cannot read or list
  await assertFails(otherDb.collection("orders").doc("ord_1").get());
  await assertFails(otherDb.collection("orders").get());

  // Customer and Seller filtered queries succeed
  await assertSucceeds(custDb.collection("orders").where("customerId", "==", CUSTOMER_UID).get());
  await assertSucceeds(sellerDb.collection("orders").where("sellerUids", "array-contains", SELLER_UID).get());

  // Seller updates order to Delivered with 10% commission (200 Rs commission + 1800 Rs seller earning)
  await assertSucceeds(
    sellerDb.collection("orders").doc("ord_1").update({
      status: "Delivered",
      commissionAmount: 200,
      sellerEarningAmount: 1800,
      payoutStatus: "PENDING_PAYOUT",
      items: [
        {
          productId: "prod_1",
          sellerUid: SELLER_UID,
          shopName: "Kashmir Craft",
          productName: "Custom Mug",
          imageUri: "preset:custom_mug",
          unitPrice: 2000,
          quantity: 1,
          lineTotal: 2000,
          commissionAmount: 200,
          sellerEarningAmount: 1800,
          payoutStatus: "PENDING_PAYOUT",
          customName: "Mujtaba",
          customPhotoUri: "",
        },
      ],
      updatedAt: pastDate(200),
    })
  );

  // Terminal state lock: Seller cannot modify Delivered order again
  await assertFails(
    sellerDb.collection("orders").doc("ord_1").update({
      status: "Pending",
      updatedAt: pastDate(100),
    })
  );

  // Admin CAN send payment to seller (payoutStatus -> PAID)
  await assertSucceeds(
    adminDb.collection("orders").doc("ord_1").update({
      payoutStatus: "PAID",
      updatedAt: pastDate(50),
    })
  );
});

test("Future timestamp tampering is rejected", async () => {
  const custDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const future = new Date(Date.now() + 60000);
  await assertFails(
    custDb.collection("users").doc(CUSTOMER_UID).collection("cart").doc("c1").set({
      userId: CUSTOMER_UID,
      productId: "prod_1",
      sellerUid: SELLER_UID,
      productName: "Mug",
      shopName: "Shop",
      unitPrice: 500,
      imageUri: "preset:custom_mug",
      quantity: 1,
      createdAt: future,
      updatedAt: future,
    })
  );
});
