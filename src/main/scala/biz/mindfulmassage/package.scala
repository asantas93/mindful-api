package biz
import com.typesafe.config.{Config, ConfigFactory}

package object mindfulmassage {
  val conf: Config = ConfigFactory.load()

  case class InvalidUserInput(msg: String) extends IllegalArgumentException(msg)
}
