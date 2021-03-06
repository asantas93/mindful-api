package biz.mindfulmassage.services

import java.io.InputStream
import java.util.Locale

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.{FileMetadata, WriteMode}

class Dropbox {

  private val conf = DbxRequestConfig.newBuilder("mm-api")
    .withUserLocaleFrom(Locale.US)
    .withAutoRetryEnabled(3)
    .build()

  private val client =
    new DbxClientV2(conf, biz.mindfulmassage.conf.getString("dropbox.token"))

  def download(path: String): InputStream = {
    client.files.download(path).getInputStream
  }

  def upload(path: String, inputStream: InputStream): FileMetadata = {
    client.files.uploadBuilder(path).withMode(WriteMode.OVERWRITE).uploadAndFinish(inputStream)
  }

}
