import app.bram.BramAccess
import app.mem.MemAccessByAXI
import chisel3.RawModule

object EmittedModule {
  // Chisel Module need to be created in a certain `builder context`
  // So the function object format is necessary
  // Rather than just Class instantiation
  def ExposedModules: List[() => RawModule] = List(
    () => new MemAccessByAXI,
    () => new BramAccess,
  )
}
