import app.bram.BramAccess
import app.mem.{MemAXILite, MemAccessByAXI}
import chisel3.RawModule
import tools.bus.{AXI4MasterModule, AXI4SlaveModule}

object EmittedModule {
  // Chisel Module need to be created in a certain `builder context`
  // So the function object format is necessary
  // Rather than just Class instantiation
  def ExposedModules: List[() => RawModule] = List(
    () => new AXI4MasterModule(32, 32),
    () => new AXI4SlaveModule(32, 32),
    () => new MemAccessByAXI,
    () => new MemAXILite,
    () => new BramAccess,
  )
}
