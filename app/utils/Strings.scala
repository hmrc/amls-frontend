package utils

object Strings {

  implicit class ConsoleHelpers(s: String) {
    def withColour(colour: String) =s"$colour$s${Console.RESET}"
  }

}
