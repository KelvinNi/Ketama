package ly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class KetamaNodeLocator implements NodeLocator {

	private volatile TreeMap<Long, KetamaNode> ketamaNodes;

	public TreeMap<Long, KetamaNode> getKetamaNodes() {
		return ketamaNodes;
	}

	private volatile Collection<KetamaNode> allNodes;

	public Collection<KetamaNode> getAllNodes() {
		return allNodes;
	}

	private final HashAlgorithm hashAlg;
	private final KetamaNodeLocatorConfiguration config;

	public KetamaNodeLocator(List<KetamaNode> nodes, HashAlgorithm alg) {
		this(nodes, alg, new DefaultKetamaNodeLocatorConfiguration());
	}

	public KetamaNodeLocator(List<KetamaNode> nodes, HashAlgorithm alg, KetamaNodeLocatorConfiguration conf) {
		allNodes = nodes;
		hashAlg = alg;
		config = conf;
		setKetamaNodes(nodes);
	}
	
	public long getMaxKey() {
		return getKetamaNodes().lastKey();
	} 

	public KetamaNode getNodeForKey(long hash) {
		if (!ketamaNodes.containsKey(hash)) {
			// �õ����ڵ�ǰkey����Map��Ȼ�����ȡ����һ��key�����Ǵ���������������Ǹ�key
			SortedMap<Long, KetamaNode> tailMap = ketamaNodes.tailMap(hash);
			if (tailMap.isEmpty()) {
				hash = ketamaNodes.firstKey();
			} else {
				hash = tailMap.firstKey();
			}
			// ceilingKey�������Է��ش���������������Ǹ�key
			// hash = ketamaNodes.ceilingKey(hash);
			// if ((Long)hash == null) {
			// hash = ketamaNodes.firstKey();
			// }
		}
		final KetamaNode rv = ketamaNodes.get(hash);
		return rv;
	}

	public Iterator<KetamaNode> getSequence(String k) {
		// Seven searches gives us a 1 in 2^7 chance of hitting the same dead
		// node all of the time.
		return new KetamaIterator(k, 7, getKetamaNodes(), hashAlg);
	}

	@Override
	public void updateLocator(List<KetamaNode> nodes) {
		allNodes = nodes;
		setKetamaNodes(nodes);
	}

	/**
	 * ��������ڵ㣬ÿ������ڵ�����numReps������ڵ㣺TreeMap
	 * <Long,KetamaNode> ÿ�ĸ�������Ϊһ�飬KetamaNodeLocatorConfiguration.
	 * getKeyForNode����Ϊ����������õ�Ωһ���� Md5��һ��16�ֽڳ��ȵ����飬��16�ֽڵ�����ÿ�ĸ��ֽ�һ�飬 �ֱ��Ӧһ��������
	 */
	protected void setKetamaNodes(List<KetamaNode> nodes) {
		TreeMap<Long, KetamaNode> newNodeMap = new TreeMap<Long, KetamaNode>();
		int numReps = config.getNodeRepetitions();
		for (KetamaNode node : nodes) {
			if (hashAlg == DefaultHashAlgorithm.KETAMA_HASH) {
				for (int i = 0; i < numReps / 4; i++) {
					byte[] digest = DefaultHashAlgorithm.computeMd5(config.getKeyForNode(node, i));
					for (int h = 0; h < 4; h++) {
						Long k = ((long) (digest[3 + h * 4] & 0xFF) << 24) | ((long) (digest[2 + h * 4] & 0xFF) << 16)
								| ((long) (digest[1 + h * 4] & 0xFF) << 8) | (digest[h * 4] & 0xFF);
						newNodeMap.put(k, node);
					}
				}
			} else {
				for (int i = 0; i < numReps; i++) {
					newNodeMap.put(hashAlg.hash(config.getKeyForNode(node, i)), node);
				}
			}
		}
		ketamaNodes = newNodeMap;
	}

	public static void main(String[] arg) {
		ArrayList<KetamaNode> alkn = new ArrayList<KetamaNode>();
		alkn.add(new KetamaNode("192.168.10.1"));
		alkn.add(new KetamaNode("192.168.10.2"));
		alkn.add(new KetamaNode("192.168.10.3"));
		alkn.add(new KetamaNode("192.168.10.4"));
		KetamaNodeLocator knl = new KetamaNodeLocator(alkn, DefaultHashAlgorithm.KETAMA_HASH);

		TreeMap<Long, KetamaNode> tm = knl.getKetamaNodes();

		Iterator<Long> it = tm.keySet().iterator();
		while (it.hasNext()) {
			Long l = it.next();
			System.out.println(l.toString() + " " + tm.get(l).getSocketAddress());
		}

		HashMap<String, Integer> hm = new HashMap<String, Integer>();
		hm.put("192.168.10.1", 0);
		hm.put("192.168.10.2", 0);
		hm.put("192.168.10.3", 0);
		hm.put("192.168.10.4", 0);

		for (int i = 0; i < 10000; i++) {
			String s = KeyUtil.getRandomString(20);
			String sa = knl.getNodeForKey(KeyUtil.bytes2Long(KeyUtil.getKeyBytes(s))%knl.getMaxKey()).getSocketAddress();
			hm.put(sa, hm.get(sa) + 1);
		}
		System.out.println("");
	}

}
