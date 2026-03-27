# API Testing Guide

Ordered API runbook for testing the `TimeSheetLeaveManagementSystem`.

## Base URLs

- Gateway base URL: [http://localhost:8080](http://localhost:8080)
- Auth docs: [http://localhost:8080/api/v1/auth/api-docs](http://localhost:8080/api/v1/auth/api-docs)
- Timesheet docs: [http://localhost:8080/api/v1/timesheets/api-docs](http://localhost:8080/api/v1/timesheets/api-docs)
- Leave docs: [http://localhost:8080/api/v1/leave/api-docs](http://localhost:8080/api/v1/leave/api-docs)
- Admin docs: [http://localhost:8080/api/v1/admin/api-docs](http://localhost:8080/api/v1/admin/api-docs)

## Before You Start

Current code limitations:

- All new users register as `EMPLOYEE`
- First `ADMIN` must be set from DB
- `MANAGER` role must be set from DB if needed
- Timesheet approval flow may still have issues because `timesheet-service` does not clearly populate manager ID before submit

Recommended DB setup after registration:

1. Change one user role to `ADMIN`
2. Change one user role to `MANAGER`

## Suggested Test Order

1. Register users
2. Login users
3. Change role in DB
4. Assign manager through API
5. Test auth/profile APIs
6. Test project APIs
7. Test leave setup APIs
8. Test leave request flow
9. Test admin approval flow
10. Test timesheet CRUD flow
11. Test timesheet submit flow
12. Test timesheet approval flow if approver mapping works in your environment

## Demo Test Data

### Employee Register Body

```json
{
  "fullName": "Ravi Employee",
  "email": "ravi.employee@demo.com",
  "employeeCode": "EMP-1001",
  "password": "Password@1"
}
```

### Manager Register Body

```json
{
  "fullName": "Meera Manager",
  "email": "meera.manager@demo.com",
  "employeeCode": "MGR-1002",
  "password": "Password@1"
}
```

### Admin Register Body

```json
{
  "fullName": "Arun Admin",
  "email": "arun.admin@demo.com",
  "employeeCode": "ADM-1003",
  "password": "Password@1"
}
```

## Step 1: Register Users

### 1.1 Register Employee

Method: `POST`  
URL: `http://localhost:8080/api/v1/auth/register`

Body:

```json
{
  "fullName": "Ravi Employee",
  "email": "ravi.employee@demo.com",
  "employeeCode": "EMP-1001",
  "password": "Password@1"
}
```

What to do next:

- save returned `id` as `employeeId`

### 1.2 Register Manager Candidate

Method: `POST`  
URL: `http://localhost:8080/api/v1/auth/register`

Body:

```json
{
  "fullName": "Meera Manager",
  "email": "meera.manager@demo.com",
  "employeeCode": "MGR-1002",
  "password": "Password@1"
}
```

What to do next:

- save returned `id` as `managerId`

### 1.3 Register Admin Candidate

Method: `POST`  
URL: `http://localhost:8080/api/v1/auth/register`

Body:

```json
{
  "fullName": "Arun Admin",
  "email": "arun.admin@demo.com",
  "employeeCode": "ADM-1003",
  "password": "Password@1"
}
```

What to do next:

- save returned `id` as `adminId`

## Step 2: Login Users

### 2.1 Login Employee

Method: `POST`  
URL: `http://localhost:8080/api/v1/auth/login`

Body:

```json
{
  "email": "ravi.employee@demo.com",
  "password": "Password@1"
}
```

What to do next:

- save `token` as `employeeToken`

### 2.2 Login Manager Candidate

Method: `POST`  
URL: `http://localhost:8080/api/v1/auth/login`

Body:

```json
{
  "email": "meera.manager@demo.com",
  "password": "Password@1"
}
```

What to do next:

- save `token` as `managerToken`

### 2.3 Login Admin Candidate

Method: `POST`  
URL: `http://localhost:8080/api/v1/auth/login`

Body:

```json
{
  "email": "arun.admin@demo.com",
  "password": "Password@1"
}
```

What to do next:

- save `token` as `adminToken`

## Step 3: DB Changes

Do these manually in DB:

1. Set admin candidate role to `ADMIN`
2. Set manager candidate role to `MANAGER`

After DB changes, login admin and manager again so the new roles are reflected in new JWTs.

### 3.1 Re-login Manager

Method: `POST`  
URL: `http://localhost:8080/api/v1/auth/login`

Body:

```json
{
  "email": "meera.manager@demo.com",
  "password": "Password@1"
}
```

### 3.2 Re-login Admin

Method: `POST`  
URL: `http://localhost:8080/api/v1/auth/login`

Body:

```json
{
  "email": "arun.admin@demo.com",
  "password": "Password@1"
}
```

## Step 4: Test Auth APIs

### 4.1 Get Employee Profile

Method: `GET`  
URL: `http://localhost:8080/api/v1/auth/profile/{employeeId}`

Headers:

```http
Authorization: Bearer <employeeToken>
```

What to do next:

- confirm profile data is correct

### 4.2 Update Employee Profile

Method: `PUT`  
URL: `http://localhost:8080/api/v1/auth/profile/{employeeId}`

Headers:

```http
Authorization: Bearer <employeeToken>
Content-Type: application/json
```

Body:

```json
{
  "fullName": "Ravi Employee Updated",
  "email": "ravi.employee.updated@demo.com"
}
```

What to do next:

- if email changed, use the new email for later employee login calls

### 4.3 Assign Manager

Method: `PUT`  
URL: `http://localhost:8080/api/v1/auth/admin/users/{employeeId}/manager`

Headers:

```http
Authorization: Bearer <adminToken>
Content-Type: application/json
```

Body:

```json
{
  "managerId": "{managerId}"
}
```

What to do next:

- assign the employee to the manager using the new API

### 4.4 Get Manager Mapping

Method: `GET`  
URL: `http://localhost:8080/api/v1/auth/users/{employeeId}/manager`

Headers:

```http
Authorization: Bearer <adminToken>
```

What to do next:

- confirm response equals `managerId`

### 4.5 Optional Admin Update User

Method: `PUT`  
URL: `http://localhost:8080/api/v1/auth/admin/users/{employeeId}`

Headers:

```http
Authorization: Bearer <adminToken>
Content-Type: application/json
```

Body:

```json
{
  "fullName": "Ravi Employee Updated",
  "email": "ravi.employee.updated@demo.com",
  "employeeCode": "EMP-1001",
  "role": "EMPLOYEE",
  "status": "ACTIVE"
}
```

## Step 5: Test Project APIs

### 5.1 Create Project

Method: `POST`  
URL: `http://localhost:8080/api/v1/projects`

Headers:

```http
Authorization: Bearer <adminToken>
Content-Type: application/json
```

Body:

```json
{
  "code": "PROJ001",
  "name": "Internal Portal Revamp",
  "description": "Demo project for API testing"
}
```

What to do next:

- save returned `id` as `projectId`

### 5.2 Get All Projects

Method: `GET`  
URL: `http://localhost:8080/api/v1/projects`

Headers:

```http
Authorization: Bearer <employeeToken>
```

### 5.3 Get Project By ID

Method: `GET`  
URL: `http://localhost:8080/api/v1/projects/{projectId}`

Headers:

```http
Authorization: Bearer <employeeToken>
```

### 5.4 Update Project

Method: `PUT`  
URL: `http://localhost:8080/api/v1/projects/{projectId}`

Headers:

```http
Authorization: Bearer <adminToken>
Content-Type: application/json
```

Body:

```json
{
  "code": "PROJ001",
  "name": "Internal Portal Revamp Phase 2",
  "description": "Updated project details"
}
```

### 5.5 Deactivate Project

Method: `PATCH`  
URL: `http://localhost:8080/api/v1/projects/{projectId}/deactivate`

Headers:

```http
Authorization: Bearer <adminToken>
```

What to do next:

- optionally create another active project if you still want one for timesheet testing

## Step 6: Test Leave Setup APIs

### 6.1 Create Leave Policy

Method: `POST`  
URL: `http://localhost:8080/api/v1/leave/policies`

Headers:

```http
Authorization: Bearer <adminToken>
Content-Type: application/json
```

Body:

```json
{
  "leaveType": "ANNUAL",
  "daysAllowed": 20,
  "carryForwardAllowed": true,
  "maxCarryForwardDays": 5,
  "requiresDelegate": false,
  "minDaysNotice": 2,
  "active": true
}
```

### 6.2 Get All Leave Policies

Method: `GET`  
URL: `http://localhost:8080/api/v1/leave/policies`

Headers:

```http
Authorization: Bearer <employeeToken>
```

### 6.3 Get Leave Policy By Type

Method: `GET`  
URL: `http://localhost:8080/api/v1/leave/policies/ANNUAL`

Headers:

```http
Authorization: Bearer <employeeToken>
```

### 6.4 Create Holiday

Method: `POST`  
URL: `http://localhost:8080/api/v1/leave/holidays`

Headers:

```http
Authorization: Bearer <adminToken>
Content-Type: application/json
```

Body:

```json
{
  "date": "2026-12-25",
  "name": "Christmas",
  "type": "MANDATORY"
}
```

What to do next:

- save holiday `id` if you plan to delete it later

### 6.5 Get Holidays

Method: `GET`  
URL: `http://localhost:8080/api/v1/leave/holidays`

Headers:

```http
Authorization: Bearer <employeeToken>
```

### 6.6 Initialize Leave Balances

Method: `POST`  
URL: `http://localhost:8080/api/v1/leave/balances/initialize?employeeId={employeeId}`

Headers:

```http
Authorization: Bearer <adminToken>
```

### 6.7 Get Employee Leave Balances

Method: `GET`  
URL: `http://localhost:8080/api/v1/leave/balances`

Headers:

```http
Authorization: Bearer <employeeToken>
```

What to do next:

- confirm annual/sick/unpaid balances exist

## Step 7: Test Leave Request Flow

### 7.1 Create Leave Request

Method: `POST`  
URL: `http://localhost:8080/api/v1/leave/requests`

Headers:

```http
Authorization: Bearer <employeeToken>
Content-Type: application/json
```

Body:

```json
{
  "leaveType": "ANNUAL",
  "startDate": "2026-03-30",
  "endDate": "2026-03-31",
  "reason": "Family function"
}
```

What to do next:

- save returned `id` as `leaveRequestId`

### 7.2 Get My Leave Requests

Method: `GET`  
URL: `http://localhost:8080/api/v1/leave/requests`

Headers:

```http
Authorization: Bearer <employeeToken>
```

### 7.3 Get Team Calendar

Method: `GET`  
URL: `http://localhost:8080/api/v1/leave/team-calendar`

Headers:

```http
Authorization: Bearer <managerToken>
```

What to do next:

- confirm employee leave appears for manager/admin side

## Step 8: Test Admin Approval Flow

### 8.1 Get Pending Approval Tasks

Method: `GET`  
URL: `http://localhost:8080/api/v1/admin/approvals/pending`

Headers:

```http
Authorization: Bearer <managerToken>
```

What to do next:

- find leave approval task
- save returned task `id` as `approvalTaskId`

### 8.2 Approve Task

Method: `POST`  
URL: `http://localhost:8080/api/v1/admin/approvals/{approvalTaskId}/approve`

Headers:

```http
Authorization: Bearer <managerToken>
Content-Type: application/json
```

Body:

```json
"Approved from admin workflow"
```

Alternative rejection:

Method: `POST`  
URL: `http://localhost:8080/api/v1/admin/approvals/{approvalTaskId}/reject`

Body:

```json
"Rejected from admin workflow"
```

### 8.3 Re-check Leave Request Status

Method: `GET`  
URL: `http://localhost:8080/api/v1/leave/requests`

Headers:

```http
Authorization: Bearer <employeeToken>
```

What to do next:

- confirm leave status became `APPROVED` or `REJECTED`

## Step 9: Test Admin Dashboard APIs

### 9.1 Get Compliance Dashboard

Method: `GET`  
URL: `http://localhost:8080/api/v1/admin/dashboard/compliance`

Headers:

```http
Authorization: Bearer <managerToken>
```

### 9.2 Get Employee Summary Dashboard

Method: `GET`  
URL: `http://localhost:8080/api/v1/admin/dashboard/employee-summary`

Headers:

```http
Authorization: Bearer <employeeToken>
```

### 9.3 Get Utilization Report

Method: `GET`  
URL: `http://localhost:8080/api/v1/admin/reports/utilization`

Headers:

```http
Authorization: Bearer <adminToken>
```

Note:

- some values are mocked in current code

## Step 10: Test Timesheet Entry Flow

Use an active project for this section. If you deactivated the earlier project, create a new one first.

### 10.1 Add Monday Entry

Method: `POST`  
URL: `http://localhost:8080/api/v1/timesheets/entries`

Headers:

```http
Authorization: Bearer <employeeToken>
Content-Type: application/json
```

Body:

```json
{
  "workDate": "2026-03-23",
  "projectId": "{projectId}",
  "hoursWorked": 8,
  "taskSummary": "Worked on module A",
  "taskId": "TASK-101",
  "activityId": "DEV"
}
```

What to do next:

- save returned `id` as `timesheetEntryId` if needed
  
### 10.2 Add Tuesday Entry

Method: `POST`  
URL: `http://localhost:8080/api/v1/timesheets/entries`

Body:

```json
{
  "workDate": "2026-03-24",
  "projectId": "{projectId}",
  "hoursWorked": 8,
  "taskSummary": "Worked on module B",
  "taskId": "TASK-102",
  "activityId": "DEV"
}
```

### 10.3 Add Wednesday Entry

Method: `POST`  
URL: `http://localhost:8080/api/v1/timesheets/entries`

Body:

```json
{
  "workDate": "2026-03-25",
  "projectId": "{projectId}",
  "hoursWorked": 8,
  "taskSummary": "Worked on module C",
  "taskId": "TASK-103",
  "activityId": "DEV"
}
```

### 10.4 Add Thursday Entry

Method: `POST`  
URL: `http://localhost:8080/api/v1/timesheets/entries`

Body:

```json
{
  "workDate": "2026-03-26",
  "projectId": "{projectId}",
  "hoursWorked": 8,
  "taskSummary": "Worked on module D",
  "taskId": "TASK-104",
  "activityId": "DEV"
}
```

### 10.5 Add Friday Entry

Method: `POST`  
URL: `http://localhost:8080/api/v1/timesheets/entries`

Body:

```json
{
  "workDate": "2026-03-27",
  "projectId": "{projectId}",
  "hoursWorked": 8,
  "taskSummary": "Worked on module E",
  "taskId": "TASK-105",
  "activityId": "DEV"
}
```

### 10.6 Get Week Timesheet

Method: `GET`  
URL: `http://localhost:8080/api/v1/timesheets?date=2026-03-23`

Headers:

```http
Authorization: Bearer <employeeToken>
```

What to do next:

- save returned timesheet `id` as `timesheetId`

### 10.7 Validate Week

Method: `GET`  
URL: `http://localhost:8080/api/v1/timesheets/weeks/2026-03-23/validate`

Headers:

```http
Authorization: Bearer <employeeToken>
```

What to do next:

- confirm `valid = true`

### 10.8 Get All Timesheets

Method: `GET`  
URL: `http://localhost:8080/api/v1/timesheets/all`

Headers:

```http
Authorization: Bearer <employeeToken>
```

### 10.9 Update One Entry

Method: `PUT`  
URL: `http://localhost:8080/api/v1/timesheets/entries/{timesheetEntryId}`

Headers:

```http
Authorization: Bearer <employeeToken>
Content-Type: application/json
```

Body:

```json
{
  "workDate": "2026-03-23",
  "projectId": "{projectId}",
  "hoursWorked": 7.5,
  "taskSummary": "Worked on module A and bug fixes",
  "taskId": "TASK-101",
  "activityId": "DEV"
}
```

## Step 11: Submit Timesheet

### 11.1 Submit Week

Method: `POST`  
URL: `http://localhost:8080/api/v1/timesheets/submit`

Headers:

```http
Authorization: Bearer <employeeToken>
Content-Type: application/json
```

Body:

```json
{
  "weekStart": "2026-03-23",
  "comments": "Week complete and ready for approval"
}
```

What to do next:

- check returned status
- then check admin pending approval task

## Step 12: Test Timesheet Approval Flow

This may or may not work depending on whether approver ID is actually being populated in your runtime data.

### 12.1 Get Pending Approval Tasks

Method: `GET`  
URL: `http://localhost:8080/api/v1/admin/approvals/pending`

Headers:

```http
Authorization: Bearer <managerToken>
```

What to do next:

- if timesheet task appears, save task id
- if not, the timesheet approver mapping issue is likely blocking the flow

### 12.2 Approve Timesheet Task

Method: `POST`  
URL: `http://localhost:8080/api/v1/admin/approvals/{approvalTaskId}/approve`

Headers:

```http
Authorization: Bearer <managerToken>
Content-Type: application/json
```

Body:

```json
"Approved timesheet"
```

### 12.3 Re-check Timesheet

Method: `GET`  
URL: `http://localhost:8080/api/v1/timesheets?date=2026-03-23`

Headers:

```http
Authorization: Bearer <employeeToken>
```

What to do next:

- confirm final status is `APPROVED` or `REJECTED`

## Optional Direct Approval Endpoints

These bypass admin approval task flow and hit service endpoints directly.

### Approve Leave Directly

Method: `PATCH`  
URL: `http://localhost:8080/api/v1/leave/requests/{leaveRequestId}/approve`

Headers:

```http
Authorization: Bearer <managerToken>
Content-Type: application/json
```

Body:

```json
{
  "comments": "Approved"
}
```

### Reject Leave Directly

Method: `PATCH`  
URL: `http://localhost:8080/api/v1/leave/requests/{leaveRequestId}/reject`

Headers:

```http
Authorization: Bearer <managerToken>
Content-Type: application/json
```

Body:

```json
{
  "comments": "Rejected"
}
```

### Cancel Leave

Method: `PATCH`  
URL: `http://localhost:8080/api/v1/leave/requests/{leaveRequestId}/cancel`

Headers:

```http
Authorization: Bearer <employeeToken>
```

### Approve Timesheet Directly

Method: `PATCH`  
URL: `http://localhost:8080/api/v1/timesheets/{timesheetId}/approve`

Headers:

```http
Authorization: Bearer <managerToken>
Content-Type: application/json
```

Body:

```json
{
  "comments": "Approved"
}
```

### Reject Timesheet Directly

Method: `PATCH`  
URL: `http://localhost:8080/api/v1/timesheets/{timesheetId}/reject`

Headers:

```http
Authorization: Bearer <managerToken>
Content-Type: application/json
```

Body:

```json
{
  "comments": "Need correction"
}
```

## Useful Token Header Templates

### Employee Header

```http
Authorization: Bearer <employeeToken>
```

### Manager Header

```http
Authorization: Bearer <managerToken>
```

### Admin Header

```http
Authorization: Bearer <adminToken>
```

## Recommended Postman Variables

- `baseUrl`
- `employeeId`
- `managerId`
- `adminId`
- `employeeToken`
- `managerToken`
- `adminToken`
- `projectId`
- `timesheetId`
- `timesheetEntryId`
- `leaveRequestId`
- `approvalTaskId`

## Known Testing Notes

- `role` values: `EMPLOYEE`, `MANAGER`, `ADMIN`
- `status` values: `ACTIVE`, `INACTIVE`, `LOCKED`
- `leaveType` values: `CASUAL`, `SICK`, `EARNED`, `OPTIONAL`, `COMP_OFF`, `UNPAID`, `ANNUAL`
- `holidayType` values: `MANDATORY`, `OPTIONAL`
- manager assignment API: `PUT /api/v1/auth/admin/users/{id}/manager`
- admin approve/reject API expects a raw JSON string body, not an object
- leave/timesheet direct approve and reject APIs expect JSON object body with `comments`
