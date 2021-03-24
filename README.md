# Boomflow

**Boomflow** is a Java SDK to interact with token contracts on chain. User could easily build decentralized exchange (DEX) based on **Boomflow** SDK. **Boomflow** provides several functionalities in common:

- Monitor event logs.
- Verify `EIP712` signature.
- Asynchronous settlement on chain.
- Transaction relayers.
- Monitor transaction confirmation.

## Packages

- `common`: common utilities, e.g. `CfxBuilder`, `SignUtils`.
- `eip712`: EIP712 core functions and typed data definitions.
- `log`: event log monitor and handler.
- `worker`: asynchronous settlement on chain.

## Monitor Event Logs

Generally, there are 2 kinds of event logs to monitor on chain: `Deposit` and `Withdraw`. Application could integrate `EventLogMonitor` within a service bean or in a scheduled periodical task. On the other hand, application need to provide a `EventLogHandler` to handle the polled event logs, e.g. update account balances in database. `MonitorEventLogDemo` under examples folder shows how to monitor and handle event logs.

## Verify EIP712 signature

Username and password are not required anymore in a DEX. Instead, any user operation always comes with a signature.
Generally, EIP712 signature are widely used and should be verfied at backend service and smart contract on chain.

**Boomflow** provides following typed data to support EIP712 verification:

- `TypedOrder`
- `TypedOrderCancellation`
- `TypedMarginRateAdjustment`
- `TypedWithdraw`

`EIP712Demo` under examples folder shows how to use typed data to validate EIP712 signature from client side.

## Asynchronous Settlement on Chain

Application could create one or multiple `SettlementWorker` for asynchronous settlement on chain. On the other hand, 
application should provide necessary `SettlementHandler` to handle exceptions when something goes wrong. Besides, to make sure transaction propagated and executed timely, we could configure `TransactionRelayer` with multiple RPC servers to achieve better transaction propagation. `SettlementDemo` under examples folder shows how to build `SettlementWorker` to settle data asynchronously.