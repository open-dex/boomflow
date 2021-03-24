package boomflow.common;

import java.util.Arrays;
import java.util.Collections;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.utils.Numeric;

public class EncodeUtils {
	
	public static String encode(String method, Type<?>... params) {
		Function func = new Function(method, Arrays.asList(params), Collections.emptyList());
		return FunctionEncoder.encode(func);
	}
	
	public static DynamicBytes hex2Bytes(String hex) {
		return new DynamicBytes(Numeric.hexStringToByteArray(hex));
	}

}
