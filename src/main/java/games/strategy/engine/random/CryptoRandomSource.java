package games.strategy.engine.random;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.vault.Vault;
import games.strategy.engine.vault.VaultID;

/**
 * A random source that generates numbers using a secure algorithm shared
 * between two players.
 * Code originally contributed by Ben Giddings.
 */
public class CryptoRandomSource implements IRandomSource {
  private final IRandomSource m_plainRandom = new PlainRandomSource();

  /**
   * Converts an {@code int} array to a {@code byte} array. Each {@code int} will be encoded in little endian order in
   * the {@code byte} array.
   */
  @VisibleForTesting
  static byte[] intsToBytes(final int[] ints) {
    final ByteBuffer byteBuffer = ByteBuffer.allocate(ints.length * 4).order(ByteOrder.LITTLE_ENDIAN);
    byteBuffer.asIntBuffer().put(ints);
    final byte[] bytes = new byte[byteBuffer.remaining()];
    byteBuffer.get(bytes);
    return bytes;
  }

  /**
   * Converts a {@code byte} array to an {@code int} array. The {@code byte} array is assumed to contain {@code int}s
   * encoded in little endian order.
   */
  static int[] bytesToInts(final byte[] bytes) {
    final ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length).order(ByteOrder.LITTLE_ENDIAN);
    byteBuffer.put(bytes);
    byteBuffer.rewind();
    final IntBuffer intBuffer = byteBuffer.asIntBuffer();
    final int[] ints = new int[intBuffer.remaining()];
    intBuffer.get(ints);
    return ints;
  }

  /**
   * Mixes the values from the specified sources ensuring that all of the mixed values are in the range [0, max).
   *
   * @param val1 The first source of values.
   * @param val2 The second source of values.
   * @param max The maximum mixed value, exclusive.
   *
   * @return The mixed values.
   *
   * @throws IllegalArgumentException If {@code val1} and {@code val2} have different lengths.
   */
  static int[] mix(final int[] val1, final int[] val2, final int max) {
    if (val1.length != val2.length) {
      throw new IllegalArgumentException("Arrays not of same length");
    }

    final int[] mixedValues = new int[val1.length];
    for (int i = 0; i < val1.length; i++) {
      mixedValues[i] = (val1[i] + val2[i]) % max;
    }
    return mixedValues;
  }

  // the remote players who involved in rolling the dice
  // dice are rolled securly between us and her
  private final PlayerID m_remotePlayer;
  private final IGame m_game;

  public CryptoRandomSource(final PlayerID remotePlayer, final IGame game) {
    m_remotePlayer = remotePlayer;
    m_game = game;
  }

  /**
   * All delegates should use random data that comes from both players so that
   * neither player cheats.
   */
  @Override
  public int getRandom(final int max, final String annotation) throws IllegalArgumentException, IllegalStateException {
    return getRandom(max, 1, annotation)[0];
  }

  /**
   * Delegates should not use random data that comes from any other source.
   */
  @Override
  public int[] getRandom(final int max, final int count, final String annotation)
      throws IllegalArgumentException, IllegalStateException {
    if (count <= 0) {
      throw new IllegalArgumentException("Invalid count:" + count);
    }
    final Vault vault = m_game.getVault();
    // generate numbers locally, and put them in the vault
    final int[] localRandom = m_plainRandom.getRandom(max, count, annotation);
    // lock it so the client knows that its there, but cant read it
    final VaultID localId = vault.lock(intsToBytes(localRandom));
    // ask the remote to generate numbers
    final IRemoteRandom remote =
        (IRemoteRandom) (m_game.getRemoteMessenger().getRemote(ServerGame.getRemoteRandomName(m_remotePlayer)));
    final Object clientRandom = remote.generate(max, count, annotation, localId);
    if (!(clientRandom instanceof int[])) {
      // Let the error be thrown
      System.out.println("Client remote random generated: " + clientRandom + ".  Asked for: " + count + "x" + max
          + " for " + annotation);
    }
    final int[] remoteNumbers = (int[]) clientRandom;
    // unlock ours, tell the client he can verify
    vault.unlock(localId);
    remote.verifyNumbers();
    // finally, we join the two together to get the real value
    return mix(localRandom, remoteNumbers, max);
  }
}
