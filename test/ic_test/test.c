int b = 1;

int main()
{
	int a1 = 1;
	int a2 = 2;
	int res;
	// unary oper
	res = !a1;
	res = ~a1;
	// binary oper
	res = a1 + a2;
	res = a1 % a2;
	res = a1 << a2;
	// selfchange
	res = a1++;
	res = ++a1;
	// short-circuit evaluation and "if" control-flow
	if (a1 > a2)
	{
		res = a1 + a2;
	}
	else
	{
		// b is global
		res = b + a2;
	}
	// "for" control-flow
	int i;
	for (i = 0; i < a1; i++)
	{
		b = b * 2;
		res += 1;
	}
	return 0;
}