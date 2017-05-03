package controllers.responsiblepeople

import scala.util.Random

trait NinoUtil {

  def nextNino: String = {

    val random = new Random
    val prefix = "AA"
    val number = random.nextInt(1000000)
    val suffix = "A"
    f"$prefix$number%06d$suffix"
  }

}
