import app.halftone.errdiff.ErrorOut
import app.halftone.{ErrDiffConfig, ErrDiffCore}
import chisel3.RawModule

object EmittedModule {
  // Chisel Module needs to be created in a certain `builder context`
  // So the function object format is necessary
  // Rather than just Class instantiation
  def ExposedModules: List[() => RawModule] = List(
//    () => new MemAccessByAXI,
//    () => new AXI4MasterModule(32, 32),
//    () => new AXI4SlaveModule(32, 32),
//    () => new BramAccess,
    () => new ErrorOut(ErrDiffConfig()),
    () => new ErrDiffCore(ErrDiffConfig()),
  )
}
