# API-Referenz: BVI RH-SSO EAI API Version v1.7.x

## Schnittstellenbeschreibung Benutzerverwaltung-EAI

### Version:
- **v1.7.x**

### Datum:
- **29. Juli 2020**

### Status:
- **Entwurf**

### Erstellt von:
- Roland Werner (ITM-KM81)
- Angel Gerdzhikov

---

### Änderungsnachweis

| Datum       | Erstellt von       | Version | Aktivität                                                                 |
|-------------|---------------------|---------|--------------------------------------------------------------------------|
| 31.03.2020  | Roland Werner       | 0.1     | Initiale Befüllung                                                      |
| 14.05.2020  | Angel Gerdzhikov    | 1.4.x   | Vervollständigen des Methodensatzes, Formatierung                        |
| 18.05.2020  | Angel Gerdzhikov    | 1.5.x   | Versionisierungsbeschreibung hinzugefügt                                 |
| 22.05.2020  | Angel Gerdzhikov    | 1.6.x   | Response-Type für alle Responses definiert                              |
| 28.05.2020  | Angel Gerdzhikov    | 1.7.x   | GET /compositeRoles Antwort-Beispiel aktualisiert                        |
| 13.07.2020  | Angel Gerdzhikov    | 1.7.x   | F112-ErrorCode hinzugefügt;Result-Code 1020 hinzugefügt                  |

---

### Inhaltsverzeichnis
1. [Methoden](#methoden)
2. [Allgemeine Response-Form](#allgemeine-response-form)
3. [GET / (API Info)](#get-api-info)
4. [POST /resourceToRoles](#post-resourcetoroles)
5. [GET /compositeRoles](#get-compositeroles)
6. [DELETE /deleteResource/{name}](#delete-deleteresource-name)
7. [GET /resourcesForUser](#get-resourcesforuser)
8. [GET /userInfo](#get-userinfo)
9. [PUT /user/{id}/password](#put-user-id-password)
10. [Error-Handling](#error-handling)
11. [Result-Codes](#result-codes)
12. [Error-Codes](#error-codes)

---

## Methoden

- Erfolgreicher Aufruf: HTTP-Status-Code zwischen 200 und 299.
- Fehlerhafter Aufruf: HTTP-Status-Code zwischen 400 und 599.

## Allgemeine Response-Form

Jede Methode liefert sowohl im Erfolgs- als auch im Fehlerfall eine Antwort nach einem allgemeinen Schema.

### HTTP STATUS CODE 200 - 299
### Response Model: GeneralResponse

| NAME         | TYPE   | DESCRIPTION                                                                 |
|--------------|--------|-----------------------------------------------------------------------------|
| responseType | String | Beschreibung siehe unten                                                    |
| resultCode   | Enum   | Siehe Result-Codes                                                          |
| resultText   | String | Enthält einen Ergebnistext, passend zum resultCode                          |

### Response-Type
- `api-info`
- `resource-roles-mapping`
- `roles`
- `general`
- `resources`
- `user-info`
- `user-password`
- `failure`

---

## GET / (API Info)

Liefert allgemeine Informationen über die API.

### REQUEST
- Keine Parameter

### RESPONSE

| NAME          | TYPE     | DESCRIPTION                                                                              |
|---------------|----------|------------------------------------------------------------------------------------------|
| responseType  | String   | Immer `api-info`. Siehe: Allgemeine Response-Form.                                        |
| resultCode    | Enum     | Siehe Result-Codes                                                                        |
| resultText    | String   | Beschreibung passend zum resultCode                                                       |
| ... weitere Felder ...   |

```json
{
    "responseType": "api-info",
    "resultCode": "1000",
    "resultText": "API Info erfolgreich erstellt.",
    ...
}
```

---

## POST /resourceToRoles

Funktionalität:
- Resource wird neu angelegt, falls nicht existent.
- Alle in Roles übermittelten Rollen werden per Policy der Resource zugeordnet.
- Falls die Resource bereits existiert und ihr Roles über Policies zugeordnet sind, die NICHT in Roles aufgeführt sind, werden diese Zuordnungen entfernt.

### REQUEST

```json
{
    "resource": "oktoberfestzugaenge",
    "roles": ["oktoberfestzugaenge_role"]
}
```

### RESPONSE

```json
{
    "responseType": "resource-roles-mapping",
    "resultCode": "1000",
    "resultText": "Operation erfolgreich ausgeführt.",
    "resourceCreated": false,
    ...
}
```

---

## GET /compositeRoles

### REQUEST
- Keine Parameter

### RESPONSE

```json
{
    "responseType": "roles",
    "resultCode": "1000",
    "resultText": "Liste von Composite-Roles",
    "compositeRoles": [
        {
            "name": "Rotes Kreuz",
            "composite": true,
            ...
        },
        ...
    ]
}
```

---

## DELETE /deleteResource/{name}

### REQUEST

| NAME  | TYPE   | DESCRIPTION           |
|-------|--------|-----------------------|
| name* | String | Name der zu löschenden Resource |

### RESPONSE

```json
{
    "responseType": "general",
    "resultCode": "1000",
    "resultText": "Ausführung war erfolgreich."
}
```

---

## GET /resourcesForUser

### REQUEST
- Keine Parameter

### RESPONSE

```json
{
    "responseType": "resources",
    "resultCode": "1000",
    "resultText": "Ausführung war erfolgreich.",
    "resources": [
        {
            "id": "662ee187-4a7e-4373-9a4b-d36c88ff7890",
            "name": "oktoberfestzugaenge",
            "displayName": "Oktoberfestzugänge"
        },
        ...
    ]
}
```

---

## GET /userInfo

### REQUEST
- Keine Parameter

### RESPONSE

```json
{
    "responseType": "user-info",
    "resultCode": "1000",
    "resultText": "Nutzerinformationen",
    "user": {
        "sub": "99b0a1a0-62f2-4a49-b85a-a8b89c8498c0",
        "address": {
            ...
        },
        ...
    }
}
```

---

## PUT /user/{id}/password

### REQUEST

| NAME  | TYPE   | DESCRIPTION           |
|-------|--------|-----------------------|
| id*   | UUID   | Der Identifikator eines Nutzers. |

### RESPONSE

```json
{
    "responseType": "user-password",
    "resultCode": "1000",
    "resultText": "Ausführung war erfolgreich.",
    "password": "c8119c1e-f911-448e-a45e-5405a9f45788"
}
```

---

## Error-Handling

Jede Fehler-Response folgt einem einheitlichen Schema:

### FEHLER RESPONSE MODEL

| NAME           | TYPE    | DESCRIPTION                                                                       |
|----------------|---------|-----------------------------------------------------------------------------------|
| responseType*  | String  | Im Fall eines Fehlers, wird diese Eigenschaft den Wert `failure` haben.            |
| resultCode*    | Enum    | Siehe Error-Codes                                                                  |
| resultText*    | String  | Enthält eine Beschreibung, passend zum `errorCode`.                                |
| errorCode*     | Enum    | Fehler, die die weitere Ausführung verhindert haben. Siehe: Error-Codes            |
| errorText*     | String  | Enthält eine Beschreibung, passend zum `errorCode`.                                |
| executionSteps*| Object  | Eine Map der durchgelaufenen Prozessschritte.                                       |
| errorDetails*  | String  | Enthält weitere Details zum Fehler. Kann bei Fehleranalyse herangezogen werden. |

```json
{
    "responseType": "failure",
    "resultCode": "1004",
    "resultText": "Fehler beim Check ob Roles existieren. (Schritt: de.muenchen.rhsso.routes.checkRolesExist)",
    "errorCode": "F100",
    "errorText": "Role \"roteskreuz\" konnte nicht gefunden werden.",
    "errorDetails": null,
    "executionSteps": {
        "de.muenchen.rhsso.routes.findClientId": true,
        "de.muenchen.rhsso.routes.checkRolesExist": false
    }
}
```

---

## Result-Codes

Hinweis: Diese Codes sind nicht final und können sich während der Entwicklung noch ändern.

| Result-Code | ResultText                                       | Methoden                                                                                     |
|-------------|--------------------------------------------------|---------------------------------------------------------------------------------------------|
| 1000        | Ausführung war erfolgreich.                       | Alle                                                                                         |
| 1001        | Fehler beim Zugriff auf den Client-Access-Token.  | POST /roles/{role_uuid}/resources, DELETE /roles/{role_uuid}/members, ...                    |
| 1002        | Fehler beim Parsen des Inputs.                    | POST /roles/{role_uuid}/resources, DELETE /roles/{role_uuid}/members, ...                    |
| 1003        | Fehler beim Abrufen der Client-ID.                | GET /roles/{role_uuid}/resources, DELETE /roles/{role_uuid}/resources, ...                   |
| 1004        | Fehler beim Check ob Roles existieren.            | GET /roles/{role_uuid}/resources, DELETE /roles/{role_uuid}/resources, ...                   |
| 1005        | Fehler beim Check ob Resource (bereits) existiert.| ...                                                                                           |
| 1006        | Fehler bei der Neuanlage der Resource.            | ...                                                                                           |
| 1007        | Fehler beim Abrufen der Resource-ID.              | ...                                                                                           |
| 1008        | Fehler beim Auswählen der korrekten Resource-ID.  | ...                                                                                           |
| 1009        | Fehler beim Abrufen der bereits zugeordneten Rollen. | ...                                                                                           |
| ...         | ...                                               | ...                                                                                           |

---

## Error-Codes

Hinweis: Diese Codes sind nicht final und können sich während der Entwicklung noch ändern. Error-Details sind fallspezifisch und hier nicht aufgeführt.

| ErrorCode | ErrorText                                                               |
|-----------|-------------------------------------------------------------------------|
| F100      | Role nicht gefunden: {roleName}                                         |
| F101      | Resource muss angegeben werden.                                         |
| F102      | Roles müssen angegeben werden.                                          |
| F103      | Resource nicht gefunden.                                                |
| F104      | Nutzer-ID ist ungültig.                                                 |
| F105      | Falscher Token-Typ verwendet.                                           |
| F106      | Role-ID ist ungültig.                                                   |
| F107      | Request-Body ist leer.                                                  |
| F108      | Request-Body hat ein falsches Format.                                   |
| F109      | Client Name/ID fehlt.                                                   |
| ...       | ...                                                                     |
| F900      | RH-SSO meldet unautorisierten Zugriff. Ggf. wurde der RH-SSO ohne Token aufgerufen. |
| F901      | Operation wurde vom RH-SSO aufgrund fehlender Berechtigung zurückgewiesen. |
| ...       | ...                                                                     |

---