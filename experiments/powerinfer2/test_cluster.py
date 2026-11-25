"""Synthetic mathematical regression tests, no model download."""
from concurrent.futures import ThreadPoolExecutor
import unittest
import torch
from torch.nn import functional as F
from stream_qwen import ClusterMLP

class ClusterTest(unittest.TestCase):
    def test_dense_partition(self):
        torch.manual_seed(4)
        weights = {"m.gate_proj.weight": torch.randn(11, 5),
                   "m.up_proj.weight": torch.randn(11, 5),
                   "m.down_proj.weight": torch.randn(5, 11)}
        class Store:
            def read(self, name, region):
                return weights[name][region].clone()
        x = torch.randn(1, 3, 5)
        expected = F.linear(F.silu(F.linear(x, weights["m.gate_proj.weight"])) *
                            F.linear(x, weights["m.up_proj.weight"]),
                            weights["m.down_proj.weight"])
        with ThreadPoolExecutor(max_workers=1) as io:
            for width in (1, 4, 11, 20):
                observed = ClusterMLP(Store(), "m", 11, width, io)(x)
                torch.testing.assert_close(observed, expected, rtol=1e-5, atol=1e-5)

if __name__ == "__main__":
    unittest.main()
