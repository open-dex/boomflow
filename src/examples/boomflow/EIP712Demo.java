package boomflow;

import boomflow.common.Address;
import boomflow.eip712.TypedWithdraw;
import conflux.web3j.CfxUnit;

public class EIP712Demo {
	
	public static void main(String[] args) {
		TypedWithdraw withdraw = new TypedWithdraw(
				Address.createCfxAddress("cfxtest:achu8p0e61a977wjwt9r1934h86853rp6664nu0fvn"),	// user address
				CfxUnit.cfx2Drip(3.5),							// 3.5 CFX or USDT/ETH/BTC
				Address.createCfxAddress("cfxtest:achu8p0e61a977wjwt9r1934h86853rp6664nu0fvn"),	// recipient to withdraw
				true,											// burn or not, true means withdraw to CFX, not WCFX
				System.currentTimeMillis(),						// usually comes from client side
				Address.createCfxAddress("cfxtest:achs3nehae0j6ksvy1bhrffsh1rtfrw1f6w1kzv46t"),	// WCFX address
				"signature in HEX format, length is 132 with 0x prefix"	// EIP712 signature that from client side
		);
		
		// get the hash of typed data
		System.out.println(withdraw.hashHex());
		
		// validate the EIP712 signature and return hash if succeeded.
		String hash = withdraw.validate();
		System.out.println(hash);
	}

}
