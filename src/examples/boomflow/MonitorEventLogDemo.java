package boomflow;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import boomflow.common.CfxBuilder;
import boomflow.log.DepositData;
import boomflow.log.EventLogHandler;
import boomflow.log.EventLogMonitor;
import boomflow.log.ScheduleWithdrawRequest;
import conflux.web3j.Cfx;
import conflux.web3j.types.Address;

public class MonitorEventLogDemo {
	
	public static Cfx connectTestnet() {
		return new CfxBuilder("http://test.confluxrpc.org/v2")
				.withRetry(3, 1000)
				.withCallTimeout(5000)
				.build();
	}
	
	public static void main(String[] args) {
		Cfx cfx = connectTestnet();
		
		EventLogMonitor monitor = new EventLogMonitor(cfx, new ConsoleEventLogHandler(
				17387940,
				"cfxtest:achf549sa9mxsge2dtzme1mvftm5vv69h2u13t9hew"));
		
		monitor.schedule(Executors.newSingleThreadScheduledExecutor(), 3000);
	}

}

class ConsoleEventLogHandler implements EventLogHandler {
	
	private BigInteger lastPollEpoch;
	private List<Address> tokenContracts;
	
	public ConsoleEventLogHandler(long lastPollEpoch, String... contracts) {
		// Could load from file or database
		this.lastPollEpoch = BigInteger.valueOf(lastPollEpoch);
		
		// In DEX, application may add a new asset in runtime.
		this.tokenContracts = Arrays.asList(contracts).stream()
				.map(Address::new)
				.collect(Collectors.toList());
	}

	@Override
	public BigInteger getLastPollEpoch() {
		return this.lastPollEpoch;
	}

	@Override
	public List<Address> getPollAddresses() {
		return this.tokenContracts;
	}

	@Override
	public void handleEventLogs(List<DepositData> deposits, List<ScheduleWithdrawRequest> withdraws, BigInteger lastPollEpoch) {
		// Should update users' balances and last polled epoch in a transaction
		System.out.println("==================================================");
		System.out.printf("Epoch: from = %s, to = %s\n", this.lastPollEpoch, lastPollEpoch);
		
		System.out.println("Deposits: " + deposits.size());
		for (DepositData data : deposits) {
			System.out.printf("\tsender = %s, recipient = %s, amount = %s\n", data.getSenderAddress(), data.getRecipientAddress(), data.getAmount());
		}
		
		System.out.println("Withdraws: " + withdraws.size());
		for (ScheduleWithdrawRequest data : withdraws) {
			System.out.printf("\tsender = %s, time = %s\n", data.getSenderAddress(), data.getTime());
		}
		
		this.lastPollEpoch = lastPollEpoch;
	}
	
}
