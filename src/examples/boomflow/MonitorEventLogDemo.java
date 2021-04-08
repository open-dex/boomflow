package boomflow;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import boomflow.common.Address;
import boomflow.common.CfxBuilder;
import boomflow.log.CfxEventLogMonitor;
import boomflow.log.DepositData;
import boomflow.log.EventLogHandler;
import boomflow.log.EventLogMonitor;
import boomflow.log.ScheduleWithdrawRequest;
import conflux.web3j.Cfx;

public class MonitorEventLogDemo {
	
	public static Cfx connectTestnet() {
		return new CfxBuilder("http://test.confluxrpc.org/v2")
				.withRetry(3, 1000)
				.withCallTimeout(5000)
				.build();
	}
	
	public static void main(String[] args) {
		EventLogMonitor monitor = new CfxEventLogMonitor(connectTestnet(), new ConsoleEventLogHandler(
				17387940,
				Address.createCfxAddress("cfxtest:achf549sa9mxsge2dtzme1mvftm5vv69h2u13t9hew")));
		
		monitor.schedule(Executors.newSingleThreadScheduledExecutor(), 3000);
	}

}

class ConsoleEventLogHandler implements EventLogHandler {
	
	private BigInteger lastPollBlockNumber;
	private List<Address> tokenContracts;
	
	public ConsoleEventLogHandler(long lastPollBlockNumber, Address... contracts) {
		// Could load from file or database
		this.lastPollBlockNumber = BigInteger.valueOf(lastPollBlockNumber);
		
		// In DEX, application may add a new asset in runtime.
		this.tokenContracts = Arrays.asList(contracts);
	}

	@Override
	public BigInteger getLastPollBlockNumber() {
		return this.lastPollBlockNumber;
	}

	@Override
	public List<Address> getPollAddresses() {
		return this.tokenContracts;
	}

	@Override
	public void handleEventLogs(List<DepositData> deposits, List<ScheduleWithdrawRequest> withdraws, BigInteger lastPollBlockNumber) {
		// Should update users' balances and last polled epoch in a transaction
		System.out.println("==================================================");
		System.out.printf("Epoch: from = %s, to = %s\n", this.lastPollBlockNumber, lastPollBlockNumber);
		
		System.out.println("Deposits: " + deposits.size());
		for (DepositData data : deposits) {
			System.out.printf("\tsender = %s, recipient = %s, amount = %s\n", data.getSenderAddress(), data.getRecipientAddress(), data.getAmount());
		}
		
		System.out.println("Withdraws: " + withdraws.size());
		for (ScheduleWithdrawRequest data : withdraws) {
			System.out.printf("\tsender = %s, time = %s\n", data.getSenderAddress(), data.getTime());
		}
		
		this.lastPollBlockNumber = lastPollBlockNumber;
	}
	
}
