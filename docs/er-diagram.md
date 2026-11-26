# ER-диаграмма

```mermaid
erDiagram
    EQUIPMENT ||--o{ LOANS : "выдаётся"
    EQUIPMENT {
        INTEGER id PK
        TEXT name
        TEXT type
        TEXT serial_number UK
        TEXT status
    }
    LOANS {
        INTEGER id PK
        INTEGER equipment_id FK
        TEXT borrower
        TEXT issue_date
        TEXT return_date
    }
```

Одна единица оборудования может иметь несколько выдач в истории. У каждой выдачи есть ровно одна единица оборудования. Если `return_date` пустая, выдача активна. Частичный уникальный индекс `one_active_loan` запрещает две активные выдачи одного оборудования.
