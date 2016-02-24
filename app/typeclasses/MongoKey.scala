package typeclasses

trait MongoKey[A] {
  def apply(): String
}
