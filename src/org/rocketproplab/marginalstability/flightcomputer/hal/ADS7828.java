package org.rocketproplab.marginalstability.flightcomputer.hal;

import java.io.IOException;
import com.pi4j.io.i2c.I2CDevice;

public class ADS7828 implements PollingSensor {
  private I2CDevice i2c;

  private byte usingIntRef;    // Internal 2.5V Reference (0 or 1)

  /*
   * 7-bit Device Addresses:
   * 0b10010XY
   * X = A1 pin (GND = 0)
   * Y = A0 pin (GND = 0)
   */
  public ADS7828(I2CDevice dev) {
    if ((dev.getAddress() & 0xF8) != 0b1001000) {
      throw new RuntimeException("Invalid I2C address for ADS7828: " + dev.getAddress());
    }

    this.i2c = dev;

    this.usingIntRef = 1;
  }

  /*
   * Configures the ADC to use the internal 2.5V Reference
   * or an external VRef
   *
   * newUsingIntRef values:
   *  0: Use external VRef
   *  1: Use internal 2.5V Reference
   */
  public void setUsingIntRef(boolean useIntRef) {
    usingIntRef = (byte) (useIntRef ? 1 : 0);
  }

  /*
   * Note: The ADS7827 does not have internal registers. Transactions
   * are performed by writing to the device address, and the byte that
   * follows is a single command byte that initiates an ADC conversion.
   *
   * A repeated start condition and an I2C read follows the command byte.
   *
   * `channel' in range [0, 7]
   *
   * Single-ended inputs supported only at the moment,
   * Differential input mode not implemented
   */
  public short readChannel(byte channel) {

    byte[] rawReading = new byte[2];

    // First send the command byte to initiate ADC conversion

    byte commandByte;
    commandByte  = (byte) (1 << 7);      // Single-ended input
    commandByte |= (channel << 4);      // ADC Channel
    commandByte |= (usingIntRef << 3);  // Internal VRef
    commandByte |= (1 << 2);            // Stay powered on

    try {
      this.i2c.write(commandByte);
    }
    catch (IOException e) {
      System.err.println("[ADS7827] readChannel failed to write command byte");
      return -1;
    }

    // TODO Might be an issue
    // Datasheet mentions repeated start. If repeated start not strictly
    // necessary then we are fine. Test this to ensure it's fine

    // Then, read the ADC value from the device
    try {
      this.i2c.read(rawReading, 0, 2);
    }
    catch (IOException e) {
      System.err.println("[ADS7827] readChannel failed to read ADC Value");
      return -1;
    }

    // TODO test to make sure no sign extension happens
    short convResult = (short) (((short) (rawReading[0] << 8)) | ((short) rawReading[1]));
    return convResult;
  }

  public void poll() {
    // TODO implement
  }

}
