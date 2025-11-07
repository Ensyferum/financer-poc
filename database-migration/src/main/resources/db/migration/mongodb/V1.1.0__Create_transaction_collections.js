// ================================================================
// Migration: V1.1.0__Create_transaction_collections.js
// Description: Creates the transaction collections for MongoDB
// Author: Financer Development Team
// Date: 2025-11-06
// ================================================================

// Switch to financer database
use financer;

// Create transactions collection with validation
db.createCollection("transactions", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["transactionId", "fromAccountId", "toAccountId", "amount", "type", "status", "createdAt"],
            properties: {
                transactionId: {
                    bsonType: "string",
                    description: "Unique transaction identifier"
                },
                fromAccountId: {
                    bsonType: "string",
                    description: "Source account UUID"
                },
                toAccountId: {
                    bsonType: "string",
                    description: "Destination account UUID"
                },
                amount: {
                    bsonType: "double",
                    minimum: 0.01,
                    description: "Transaction amount (must be positive)"
                },
                type: {
                    bsonType: "string",
                    enum: ["TRANSFER", "DEPOSIT", "WITHDRAWAL", "PAYMENT", "REFUND"],
                    description: "Transaction type"
                },
                status: {
                    bsonType: "string",
                    enum: ["PENDING", "PROCESSING", "COMPLETED", "FAILED", "CANCELLED"],
                    description: "Transaction status"
                },
                description: {
                    bsonType: "string",
                    maxLength: 500,
                    description: "Transaction description"
                },
                reference: {
                    bsonType: "string",
                    maxLength: 100,
                    description: "External reference"
                },
                fee: {
                    bsonType: "double",
                    minimum: 0,
                    description: "Transaction fee"
                },
                metadata: {
                    bsonType: "object",
                    description: "Additional transaction metadata"
                },
                createdAt: {
                    bsonType: "date",
                    description: "Transaction creation timestamp"
                },
                updatedAt: {
                    bsonType: "date",
                    description: "Transaction update timestamp"
                },
                processedAt: {
                    bsonType: "date",
                    description: "Transaction processing timestamp"
                },
                createdBy: {
                    bsonType: "string",
                    description: "User who created the transaction"
                },
                updatedBy: {
                    bsonType: "string",
                    description: "User who last updated the transaction"
                }
            }
        }
    }
});

// Create indexes for transactions
db.transactions.createIndex({ "transactionId": 1 }, { unique: true });
db.transactions.createIndex({ "fromAccountId": 1 });
db.transactions.createIndex({ "toAccountId": 1 });
db.transactions.createIndex({ "status": 1 });
db.transactions.createIndex({ "type": 1 });
db.transactions.createIndex({ "createdAt": -1 });
db.transactions.createIndex({ "amount": 1 });
db.transactions.createIndex({ "fromAccountId": 1, "createdAt": -1 });
db.transactions.createIndex({ "toAccountId": 1, "createdAt": -1 });

// Create transaction events collection for event sourcing
db.createCollection("transaction_events", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["eventId", "transactionId", "eventType", "eventData", "timestamp"],
            properties: {
                eventId: {
                    bsonType: "string",
                    description: "Unique event identifier"
                },
                transactionId: {
                    bsonType: "string",
                    description: "Related transaction ID"
                },
                eventType: {
                    bsonType: "string",
                    enum: ["TRANSACTION_CREATED", "TRANSACTION_VALIDATED", "TRANSACTION_PROCESSING", "TRANSACTION_COMPLETED", "TRANSACTION_FAILED", "TRANSACTION_CANCELLED"],
                    description: "Event type"
                },
                eventData: {
                    bsonType: "object",
                    description: "Event payload data"
                },
                timestamp: {
                    bsonType: "date",
                    description: "Event timestamp"
                },
                version: {
                    bsonType: "int",
                    minimum: 1,
                    description: "Event version for ordering"
                },
                userId: {
                    bsonType: "string",
                    description: "User who triggered the event"
                }
            }
        }
    }
});

// Create indexes for transaction events
db.transaction_events.createIndex({ "eventId": 1 }, { unique: true });
db.transaction_events.createIndex({ "transactionId": 1, "version": 1 });
db.transaction_events.createIndex({ "timestamp": -1 });
db.transaction_events.createIndex({ "eventType": 1 });

// Create transaction audit collection
db.createCollection("transaction_audit", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["auditId", "transactionId", "operation", "timestamp"],
            properties: {
                auditId: {
                    bsonType: "string",
                    description: "Unique audit identifier"
                },
                transactionId: {
                    bsonType: "string",
                    description: "Related transaction ID"
                },
                operation: {
                    bsonType: "string",
                    enum: ["CREATE", "UPDATE", "DELETE", "STATUS_CHANGE"],
                    description: "Operation type"
                },
                oldValues: {
                    bsonType: "object",
                    description: "Previous values"
                },
                newValues: {
                    bsonType: "object",
                    description: "New values"
                },
                timestamp: {
                    bsonType: "date",
                    description: "Audit timestamp"
                },
                userId: {
                    bsonType: "string",
                    description: "User who performed the operation"
                },
                userAgent: {
                    bsonType: "string",
                    description: "User agent information"
                },
                ipAddress: {
                    bsonType: "string",
                    description: "IP address of the operation"
                }
            }
        }
    }
});

// Create indexes for transaction audit
db.transaction_audit.createIndex({ "auditId": 1 }, { unique: true });
db.transaction_audit.createIndex({ "transactionId": 1 });
db.transaction_audit.createIndex({ "timestamp": -1 });
db.transaction_audit.createIndex({ "operation": 1 });
db.transaction_audit.createIndex({ "userId": 1 });

// Insert sample transaction data for development
db.transactions.insertOne({
    transactionId: "TXN-001-2025110601",
    fromAccountId: "550e8400-e29b-41d4-a716-446655440000",
    toAccountId: "550e8400-e29b-41d4-a716-446655440001",
    amount: 100.00,
    type: "TRANSFER",
    status: "COMPLETED",
    description: "Initial test transaction",
    reference: "TEST-001",
    fee: 1.50,
    metadata: {
        channel: "WEB",
        deviceId: "web-browser-001"
    },
    createdAt: new Date(),
    updatedAt: new Date(),
    processedAt: new Date(),
    createdBy: "SYSTEM",
    updatedBy: "SYSTEM"
});

print("MongoDB collections for transactions created successfully!");
print("Collections created: transactions, transaction_events, transaction_audit");
print("Indexes created for optimal query performance");
print("Sample data inserted for development testing");