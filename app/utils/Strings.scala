package utils

object Strings {

  implicit class ConsoleHelpers(s: String) {
    def in(colour: String) =s"$colour$s${Console.RESET}"
  }

}
