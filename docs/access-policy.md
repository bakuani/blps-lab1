# Access Policy

## Roles and Privileges

### SUBSCRIBER
- `TARIFF_READ`
- `TARIFF_CHANGE_OWN`
- `FEATURE_READ`
- `FEATURE_DISABLE_OWN`

### OPERATOR
- `TARIFF_READ`
- `TARIFF_CHANGE_ANY`
- `FEATURE_READ`
- `FEATURE_DISABLE_ANY`

### ADMIN
- `TARIFF_READ`
- `TARIFF_WRITE`
- `TARIFF_DELETE`
- `TARIFF_CHANGE_ANY`
- `FEATURE_READ`
- `FEATURE_WRITE`
- `FEATURE_DELETE`
- `FEATURE_DISABLE_ANY`
- `TARIFF_CATEGORY_READ`
- `TARIFF_CATEGORY_WRITE`
- `TARIFF_CATEGORY_DELETE`
- `TARIFF_OPTION_READ`
- `TARIFF_OPTION_WRITE`
- `TARIFF_OPTION_DELETE`
- `FEATURE_CATEGORY_READ`
- `FEATURE_CATEGORY_WRITE`
- `FEATURE_CATEGORY_DELETE`
- `SUBSCRIBER_READ`
- `SUBSCRIBER_WRITE`
- `SUBSCRIBER_DELETE`
- `SUBSCRIBER_FEATURE_READ`
- `SUBSCRIBER_FEATURE_WRITE`
- `SUBSCRIBER_FEATURE_DELETE`

## Own vs Any rule

- For operations with `subscriberId` in path, SUBSCRIBER can access only own profile.
- Own profile is resolved from JWT claim `subscriber_id`.
- OPERATOR and ADMIN are treated as `any` scope users.

## Security enforcement

- Access control is configured in Spring Security via:
  - exact `requestMatchers(HttpMethod, path)` + `hasAuthority(...)`
  - method checks `@PreAuthorize("@accessGuard.canAccessSubscriber(authentication, #p0)")` for own/any boundary.
- Policy is not derived from URL naming conventions.
