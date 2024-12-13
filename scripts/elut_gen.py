with open('./out/elut_gen', 'w+') as f:
	for n in range(-127, 128): # clockwise
		data1 = round(n * 7/16)
		data2 = round(n * 1/16)
		data3 = round(n * 5/16)
		data4 = round(n * 3/16)
		format_str = f"litErrToUInt({n}) -> errorVecLit({data1}.S, {data2}.S, {data3}.S, {data4}.S),\n"
		f.write(format_str)
