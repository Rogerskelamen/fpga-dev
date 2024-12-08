import app.bram.BramAccess
import app.halftone.errdiff.ELUT
import app.halftone.{ErrDiffConfig, ErrDiffCore}
import app.mem.MemAccessByAXI
import chisel3.RawModule

object EmittedModule {
  // Chisel Module needs to be created in a certain `builder context`
  // So the function object format is necessary
  // Rather than just Class instantiation
  def ExposedModules: List[() => RawModule] = List(
//    () => new MemAccessByAXI,
//    () => new BramAccess,
    () => new ErrDiffCore(ErrDiffConfig()),
    () => new ELUT(8)
  )
}
