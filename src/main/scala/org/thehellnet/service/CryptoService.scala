package org.thehellnet.service

import javax.crypto.spec.{IvParameterSpec, PBEKeySpec, SecretKeySpec}
import javax.crypto.{Cipher, SecretKeyFactory}
import scala.util.Try

class CryptoService(encryptionKey: String, salt: String) {

  def decrypt(message: Array[Byte]): Try[String] = Try {
    val initializationVector = Array[Byte](0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    val ivParameterSpec      = new IvParameterSpec(initializationVector)
    val keyFactory           = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val keySpec              = new PBEKeySpec(encryptionKey.toCharArray, salt.getBytes, 65536, 256)
    val secretKey            = keyFactory.generateSecret(keySpec)
    val secretKeySpec        = new SecretKeySpec(secretKey.getEncoded, "AES")
    val cipher               = Cipher.getInstance("AES/CBC/PKCS5PADDING")
    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)
    new String(cipher.doFinal(message))
  }
}
