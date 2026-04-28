# trabed-alarm-plugin

This is an alarm system that tends to interact with react apps and set alarms for users

## Install

```bash
npm install trabed-alarm-plugin
npx cap sync
```

## API

<docgen-index>

* [`echo(...)`](#echo)
* [`setAlarm(...)`](#setalarm)
* [`cancelAlarm(...)`](#cancelalarm)
* [`cancelAllAlarms()`](#cancelallalarms)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### echo(...)

```typescript
echo(options: { value: string; }) => Promise<{ value: string; }>
```

| Param         | Type                            |
| ------------- | ------------------------------- |
| **`options`** | <code>{ value: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### setAlarm(...)

```typescript
setAlarm(options: { id: string; time: number; message: string; durationMinutes?: number; }) => Promise<void>
```

| Param         | Type                                                                                  |
| ------------- | ------------------------------------------------------------------------------------- |
| **`options`** | <code>{ id: string; time: number; message: string; durationMinutes?: number; }</code> |

--------------------


### cancelAlarm(...)

```typescript
cancelAlarm(options: { id: string; }) => Promise<void>
```

| Param         | Type                         |
| ------------- | ---------------------------- |
| **`options`** | <code>{ id: string; }</code> |

--------------------


### cancelAllAlarms()

```typescript
cancelAllAlarms() => Promise<void>
```

--------------------

</docgen-api>
