package io.takamaka.tests.errors.redpayablewithoutamount2;

import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.RedGreenContract;
import io.takamaka.code.lang.RedPayable;

public class RedPayableWithoutAmount extends RedGreenContract {
	public @RedPayable @Entry void m(float amount) {};
}