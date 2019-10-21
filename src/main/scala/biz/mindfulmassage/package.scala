package biz
import com.typesafe.config.{Config, ConfigFactory}

package object mindfulmassage {
  val conf: Config = ConfigFactory.load()
}
