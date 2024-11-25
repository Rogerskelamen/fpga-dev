TOPNAME = DownCounterGen

# Directories
BUILD_DIR = ./build
VERILOG_GEN = ./verilog-gen
VSIM_DIR = ./src/main/cc/sim

TARGET = $(abspath $(VERILOG_GEN)/$(TOPNAME).v)
BIN = $(abspath $(BUILD_DIR)/$(TOPNAME))
SRC = $(abspath $(VSIM_DIR)/tb_$(TOPNAME).cpp)

# Waveform
WAVEFILE = waveform.vcd
WAVECONFIG = .gtkwave.config

# Verilog simulator
VERILATOR = verilator
VERILATOR_CFLAGS = -MMD --cc --build --trace

# Version check
IS_WINDOWS := $(findstring :, $(PATH))
ifeq ($(IS_WINDOWS),)
	OS_TYPE = Linux
else
	OS_TYPE = Windows
endif

verilog:
	sbt run
ifeq ($(OS_TYPE),Windows)
	pwsh .\process.ps1
else
	find ./verilog-gen -type f -exec sed -i -e 's/_\(aw\|ar\|w\|r\|b\)_\(\|bits_\)/_\1/g' {} +
endif

wave: sim
	@gtkwave $(WAVEFILE) -r $(WAVECONFIG)

sim: $(BIN)
	@$(BIN)

$(BIN): $(TARGET) $(SRC)
	@mkdir -p $(BUILD_DIR)
	$(VERILATOR) $(VERILATOR_CFLAGS) \
		--top-module $(TOPNAME) $^ \
		--exe --Mdir $(BUILD_DIR)/obj_dir -o $(abspath $(BIN))

clean:
	-rm -rf $(BUILD_DIR) $(WAVEFILE)
