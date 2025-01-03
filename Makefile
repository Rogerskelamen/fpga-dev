TOPNAME = DownCounterGen

# Directories
WORK_DIR    = $(PWD)
BUILD_DIR   = $(WORK_DIR)/build
VERILOG_GEN = $(WORK_DIR)/verilog-gen
VSIM_DIR    = $(WORK_DIR)/src/main/cc/sim

VSRCS = $(abspath $(VERILOG_GEN)/$(TOPNAME).v)
BIN = $(abspath $(BUILD_DIR)/$(TOPNAME))
SRCS = $(shell find $(abspath $(VSIM_DIR)/tb_$(TOPNAME)) -name "*.cpp" -or -name "*.cc" -or -name "*.cc")

# Waveform
WAVEFILE = $(WORK_DIR)/waveform.vcd
WAVECONFIG = $(WORK_DIR)/.gtkwave.config

# Verilog simulator
VERILATOR = verilator
VERILATOR_CFLAGS = -MMD --cc --build --trace
INC_PATH = $(WORK_DIR)/src/main/cc/include
INCFLAGS = $(addprefix -I, $(INC_PATH))
CXXFLAGS += $(INCFLAGS)

# Environment check
ifeq ($(OS), Windows_NT)
    OS_TYPE = Windows
else
    OS_TYPE = Unix
endif

verilog:
	sbt run
ifeq ($(OS_TYPE),Windows)
	powershell .\process.ps1
else
	find ./verilog-gen -type f -exec sed -i -e 's/_\(aw\|ar\|w\|r\|b\)_\(\|bits_\)/_\1/g' {} +
endif

wave: $(WAVEFILE)
	@gtkwave $(WAVEFILE) -r $(WAVECONFIG)

sim: $(BIN)
	@$(BIN)

$(BIN): $(VSRCS) $(SRCS)
	@mkdir -p $(BUILD_DIR)
	$(VERILATOR) $(VERILATOR_CFLAGS) \
		--top-module $(TOPNAME) $^ \
		$(addprefix -CFLAGS , $(CXXFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS)) \
		--exe --Mdir $(BUILD_DIR)/obj_dir -o $(abspath $(BIN))

clean:
	-rm -rf $(BUILD_DIR) $(WAVEFILE)
