# Campus Student Registry (CSC311 Semester Project)

## Database Configuration
This project is configured for Azure SQL Server by default using your provided JDBC connection string:

`jdbc:sqlserver://csc311server.database.windows.net:1433;database=CSC311DB;user=vasqf@csc311server;password=Uriel0128;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;`

If Azure is unavailable, the app automatically falls back to Derby (`~/Documents/CSC311_DERBY_DB`).

You can override settings with environment variables:
- `APP_DB_MODE=AZURE` or `APP_DB_MODE=DERBY`
- `APP_AZURE_JDBC=<your jdbc string>`
- `APP_DERBY_PATH=<path>`

## Implemented Required Tasks

### UI State Management
- `Edit` button disabled unless a real table record is selected.
- `Delete` button disabled unless a real table record is selected.
- `Add` button enabled only when all form fields are valid.
- `Edit/Delete` menu items are disabled (grayed out) when no valid selection exists.

### Form Enhancement
- Added advanced regex validation for all form fields:
  - first name, last name, department, email, image URL
- Replaced Major text field with a dropdown using enum `model.Major` (`CS`, `CPIS`, `ENGLISH`).

### User Feedback
- Added status message label at the bottom of main stage.
- Status updates are shown for successful add/update/delete/import/export/report operations and validation errors.

### Menu Items (CSV)
- Added Data menu:
  - Import CSV
  - Export CSV
- Implemented CSV parsing/export with quoted value handling.

### Thread Safety
- Redesigned `UserSession` to be thread-safe with synchronized preference access and synchronized session replacement.

### User Session and Preferences
- Completed sign-up page with real fields and validation.
- Implemented account creation and storage in Preferences.
- On successful sign-in, username/password/privileges are recorded in Preferences.

## Extra Credit Features
- PDF report generation for number of students by major.
- Inline table row editing with validation and DB persistence.
- Add-by-empty-row workflow: a placeholder row can be edited directly to insert a new record.

## 10% Additional Improvements (4 items)

1. Branding and Presentation (required by instruction)
- Rebranded app as **Campus Student Registry**.
- Updated stage title, login title, sign-up title, and About content for consistent identity.

2. Safer UX for destructive actions
- Added confirmation dialogs for `Delete` and `Log out`.

3. Better workflow efficiency
- Added inline table editing with immediate persistence and validation feedback.

4. Better analytics/visibility
- Added PDF major summary report output for quick sharing and submission evidence.

## Build
```bash
./mvnw -DskipTests compile
```

## Notes for Submission
- Mention that Azure SQL Server is primary and Derby is fallback.
- Mention the 4 additional improvements and why they improve usability, safety, and presentation quality.
