/*
 * ADS7828.java: HAL device driver for 12-big ADC with 8 channels
 *
 * ----------------------------------------------------------------------
 * Basic Features:
 *  - Methods to trigger a single-ended conversion on any channel
 *  - Implements PollingSensor interface, allows triggering conversion
 *    at regular intervals and storing results in buffer to be accessed
 *    on demand
 *  - No differential mode support
 *  - Allows use of external precise 2.5V reference 
 *    (see hardware layout if applicable)
 *
 * ----------------------------------------------------------------------
 * Device Datasheet: http://www.ti.com/lit/ds/symlink/ads7828.pdf
 * ----------------------------------------------------------------------
 *
 * Author:       Georges Troulis <gtroulis@ucsd.edu>
 * Last Revised: 05/30/2020
 */

package org.rocketproplab.marginalstability.flightcomputer.hal;

import java.io.IOException;
import com.pi4j.io.i2c.I2CDevice;

public class ADS7828 implements PollingSensor {
  private static final byte NUM_CHANNELS = 8;

  private I2CDevice i2c;
  private byte usingIntRef;    // Internal 2.5V Reference (0 or 1)

  private short[] channelReadings; // Channel readings buffer

  /*
   * Construct and initialize an ADS7828 ADC interface using an existing I2CDevice.
   *
   * Valid 7-bit Device Addresses:
   * 0b10010XY
   * X = A1 pin (GND = 0)
   * Y = A0 pin (GND = 0)
   *
   * Preconditions:
   *  - Parameter `dev' is a properly initialized I2CDevice with an address
   *    conforming to a valid address explained above
   */
  public ADS7828(I2CDevice dev) {
    this.i2c = dev;

    this.usingIntRef = 1;

    channelReadings = new short[NUM_CHANNELS];

    // Initially all readings are invalid
    for (int i = 0; i < NUM_CHANNELS; i++) {
      channelReadings[i] = -1;
    }
  }

  /*
   * Configures the ADC to use the internal 2.5V Reference
   * or an external VRef.
   *
   * See the Motherboard circuit schematic to determine if
   * an external 2.5V reference is present. If in doubt,
   * use the internal reference (useIntRef = calse)
   *
   * Parameters:
   *  useIntRef:
   *   false: Use external VRef
   *   true:  Use internal 2.5V Reference
   */
  public void setUsingIntRef(boolean useIntRef) {
    usingIntRef = (byte) (useIntRef ? 1 : 0);
  }

  /*
   * Performs an I/O operation to read the ADC value of a specific channel.
   *
   * It is not advised to call this; Use the getChannelReading() instead, 
   * which returns the most recently sampled channel value.
   *
   * Note: The ADS7827 does not have internal registers. Transactions
   * are performed by writing to the device address, and the byte that
   * follows is a single command byte that initiates an ADC conversion.
   * A repeated start condition and an I2C read follows the command byte.
   *
   * Single-ended inputs supported only at the moment,
   * Differential input mode not implemented
   *
   * Parameters:
   *   channel: in range [0, 7]
   *
   * Return: 12-bit integer reading
   *         -1 on I/O error
   */
  public short performReadChannel(byte channel) {

    byte[] rawReading = new byte[2];

    // First send the command byte to initiate ADC conversion

    byte commandByte;
    commandByte  = (byte) (1 << 7);     // Single-ended input
    commandByte |= (channel << 4);      // ADC Channel
    commandByte |= (usingIntRef << 3);  // Internal VRef
    commandByte |= (1 << 2);            // Stay powered on

    try {
      this.i2c.write(commandByte);
    }
    catch (IOException e) {
      System.err.println("[ADS7827] readChannel failed to write command byte");
      e.printStackTrace();
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
      e.printStackTrace();
      return -1;
    }

    // TODO test to make sure no sign extension happens
    short convResult = (short) ((rawReading[0] << 8) | rawReading[1]);
    return convResult;
  }

  /*
   * Return the most recently sampled value of the given ADC channel.
   *
   * ADC channels can be sampled at regular intervals using the `poll()'
   * method, as defined in the PollingSensor interface.
   *
   * The returned reading may be -1 if some error occured while reading
   * the sample, or if the readings are uninitialized
   */
  public short getChannelReading(byte channel) {
    return channelReadings[channel];
  }

  /*
   * Perform ADC readings and store in a buffer to be accessed later,
   * as defined in the PollingSensor interface.
   *
   * Poll all 8 ADC channels and store the measurements in the
   * channelReadings buffer.
   */
  public void poll() {
    for (byte i = 0; i < NUM_CHANNELS; i++) {
      channelReadings[i] = performReadChannel(i);
    }
  }

}
