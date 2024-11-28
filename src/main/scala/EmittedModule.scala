import app.led.{LEDChaser, PulseLight}
import chisel3.RawModule

object EmittedModule {
  // Chisel Module need to be created in a certain `builder context`
  // So the function object format is necessary
  // Rather than just Class instantiation
  def ExposedModules: List[() => RawModule] = List(
    () => new LEDChaser,
    () => new PulseLight
  )
}
