// Inicialização do MongoDB para o Sistema Financer

// Conectar ao banco financer
db = db.getSiblingDB('financer');

// Criar usuário para o serviço de orquestração
db.createUser({
  user: 'orchestration_service',
  pwd: 'orchestration123',
  roles: [
    {
      role: 'readWrite',
      db: 'financer'
    }
  ]
});

// Criar usuário para logs e eventos
db.createUser({
  user: 'event_service',
  pwd: 'event123',
  roles: [
    {
      role: 'readWrite',
      db: 'financer'
    }
  ]
});

// Criar coleções com validação de schema

// Coleção para eventos de domínio
db.createCollection('domain_events', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['eventId', 'eventType', 'aggregateId', 'timestamp', 'data'],
      properties: {
        eventId: {
          bsonType: 'string',
          description: 'ID único do evento'
        },
        eventType: {
          bsonType: 'string',
          description: 'Tipo do evento'
        },
        aggregateId: {
          bsonType: 'string',
          description: 'ID do agregado relacionado'
        },
        aggregateType: {
          bsonType: 'string',
          description: 'Tipo do agregado'
        },
        timestamp: {
          bsonType: 'date',
          description: 'Timestamp do evento'
        },
        version: {
          bsonType: 'int',
          description: 'Versão do evento'
        },
        data: {
          bsonType: 'object',
          description: 'Dados do evento'
        },
        correlationId: {
          bsonType: 'string',
          description: 'ID de correlação'
        }
      }
    }
  }
});

// Coleção para sagas de orquestração
db.createCollection('orchestration_sagas', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['sagaId', 'sagaType', 'status', 'createdAt'],
      properties: {
        sagaId: {
          bsonType: 'string',
          description: 'ID único da saga'
        },
        sagaType: {
          bsonType: 'string',
          description: 'Tipo da saga'
        },
        status: {
          bsonType: 'string',
          enum: ['STARTED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'COMPENSATING', 'COMPENSATED'],
          description: 'Status da saga'
        },
        steps: {
          bsonType: 'array',
          description: 'Passos da saga'
        },
        data: {
          bsonType: 'object',
          description: 'Dados da saga'
        },
        createdAt: {
          bsonType: 'date',
          description: 'Data de criação'
        },
        updatedAt: {
          bsonType: 'date',
          description: 'Data de atualização'
        },
        correlationId: {
          bsonType: 'string',
          description: 'ID de correlação'
        }
      }
    }
  }
});

// Criar índices para performance
db.domain_events.createIndex({ 'aggregateId': 1, 'timestamp': -1 });
db.domain_events.createIndex({ 'eventType': 1, 'timestamp': -1 });
db.domain_events.createIndex({ 'correlationId': 1 });
db.domain_events.createIndex({ 'timestamp': -1 });

db.orchestration_sagas.createIndex({ 'sagaId': 1 }, { unique: true });
db.orchestration_sagas.createIndex({ 'status': 1, 'createdAt': -1 });
db.orchestration_sagas.createIndex({ 'correlationId': 1 });

print('MongoDB inicializado com sucesso para o Sistema Financer');
print('Coleções criadas: domain_events, orchestration_sagas');
print('Usuários criados: orchestration_service, event_service');