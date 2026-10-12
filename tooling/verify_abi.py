import subprocess, re, sys, zipfile, json
jar=sys.argv[1]
classes=["com.puzzlechannel.vmtrade.VmTradeInitializer","com.puzzlechannel.vmtrade.MarketBlock"]
for cls in classes:
    p=subprocess.run(["javap","-classpath",jar,"-verbose","-p",cls],capture_output=True,text=True,check=True)
    out=p.stdout
    for token in ["SimpleMenuProvider","MenuConstructor","CreativeModeTab.builder","ItemStack(Item)"]:
        if token in out and "String "+token not in out:
            raise AssertionError(f"{cls}: forbidden ABI marker {token}")
    if re.search(r"(Methodref|InterfaceMethodref).*Component.*literal",out):
        raise AssertionError(f"{cls}: direct Component.literal linkage")
with zipfile.ZipFile(jar) as z:
    meta=json.loads(z.read("fabric.mod.json"))
    assert meta.get("accessWidener")=="opentrade.accesswidener"
    assert "opentrade.accesswidener" in z.namelist()
print("VM Trade ABI verification: OK")

with zipfile.ZipFile(jar) as z:
    data=z.read("com/puzzlechannel/vmtrade/VmTradeInitializer.class")
    # javac emits 0x11 0x03 0xE8 (sipush 1000) for stacksTo(1000).
    assert b"\x11\x03\xe8" in data, "VMCoin bytecode does not contain sipush 1000"
    print("VMCoin stack size 1000 bytecode check: OK")
