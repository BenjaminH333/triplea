package games.strategy.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * A stream used for serializing objects that may be proxyable.
 *
 * <p>
 * Before serializing an object, the stream will query the proxy registry for a proxy associated with the principal. If
 * a proxy is available, the proxy will be written to the stream instead of the principal.
 * </p>
 */
public final class ProxyableObjectOutputStream extends ObjectOutputStream {
  private final ProxyRegistry proxyRegistry;

  public ProxyableObjectOutputStream(final OutputStream out, final ProxyRegistry proxyRegistry) throws IOException {
    super(out);

    checkNotNull(out);
    checkNotNull(proxyRegistry);

    this.proxyRegistry = proxyRegistry;

    enableReplaceObject(true);
  }

  @Override
  protected Object replaceObject(final Object obj) {
    return (obj != null) ? proxyRegistry.getProxyFor(obj) : null;
  }
}
