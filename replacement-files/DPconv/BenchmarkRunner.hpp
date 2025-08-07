#include <iostream>
#include <fstream>
#include <cassert>
#include <vector>
#include <unordered_map>
#include <algorithm>
#include <iomanip>
#include <chrono>
#include <memory>
#include <thread>
#include <filesystem>
#include <atomic>
#include <cstdlib>
#include <ranges>
#include <random>

#include "DPccp.cpp"
#include "DPsub.cpp"
#include "DPconv.cpp"

#include "Util.hpp"

#include "SubsetConvolution.hpp"
#include <regex>

// Source: https://stackoverflow.com/a/18681293/10348282
inline bool endswith(const std::string& s, const std::string& suffix) { return suffix.size() > s.size() ? false : (s.rfind(suffix) == s.size() - suffix.size()); }

std::tuple<MetaInfo, std::string> optimize_query(
  std::string fn, unsigned n, QueryGraph qg, const std::vector<uint64_t>& sizes, std::vector<std::string> tns, bool analyzeRatios = false) {
  auto maxCard = *std::ranges::max_element(sizes);
  auto logMaxCard = static_cast<unsigned>(std::log2(maxCard));
  std::cerr << "Max. cardinality: " << maxCard << " log=" << logMaxCard << std::endl;

  double bounded_cout_opt = 0.0;
  double cout_opt = 0.0;

  // ------------------------------------------------- //

  auto time10 = std::chrono::high_resolution_clock::now();

  // ------------------------------------------------- //






  // Capped C_{out} with DPsub + DPconv.
  std::cerr << "[START] " << fn << " {CCAP_DPCONV}" << std::endl;
  // Note: We don't build the join tree! 
  auto [tmp_ret10, tmp_join_tree10] = runMinMaxDPconv_instant<BoostedBooleanFSC>(sizes, false);
  std::cerr << "tmp_ret10=" << tmp_ret10 << std::endl;
  auto [ret10, join_tree10] = runMinMaxDPsub_hybrid(sizes, tmp_ret10);
  bounded_cout_opt = ret10;
  std::cerr << "[STOP] " << fn << " optimal_ccap_cost=" << ret10 << std::endl;
  auto time11 = std::chrono::high_resolution_clock::now();

  // !!! Set time !!!
  auto hybrid_capped_dpsub_time = std::chrono::duration_cast<std::chrono::microseconds>(time11 - time10).count();
  
  std::cerr << "CCAP_DPCONV (time): " << std::chrono::duration_cast<std::chrono::microseconds>(time11 - time10).count() << std::endl;
  // ------------------------------------------------- //

  auto time13 = std::chrono::high_resolution_clock::now();



  // Optimal C_{out} with DPccp.
  std::regex const job_original_filename_regex(".*/[0-9][0-9][a-z].csv");
  std::smatch match;
  if (std::regex_match(fn, match, job_original_filename_regex)) {
    std::cerr << "[START] " << fn << " {COUT_DPCCP}" << std::endl;

    auto minplus_wrapper = DPccpWrapper(OptimizationType::MinPlus, qg, sizes);
    auto [ret13, join_tree13] = minplus_wrapper.runDPccp();

    cout_opt = ret13;

    std::cerr << "[STOP] " << fn << " optimal_cout_cost=" << ret13 << std::endl;
    auto time14 = std::chrono::high_resolution_clock::now();

    // !!! Set time !!!
    auto minplus_dpccp_time = std::chrono::duration_cast<std::chrono::microseconds>(time14 - time13).count();
  }
  // ------------------------------------------------- //

  join_tree10->debug(fn, tns, sizes, "ccap_opt");

  auto ret = fn + ","
          + std::to_string(n) + ","
          + std::to_string(ret10) + "," + std::to_string(hybrid_capped_dpsub_time) + ","
          + std::to_string(cout_opt);


  std::cerr << ret << std::endl;

  std::cerr << "[stop] " << fn << std::endl;

  MetaInfo meta_info;
  meta_info.cout_opt = cout_opt;
  meta_info.bounded_cout_opt = bounded_cout_opt;
  return {meta_info, ret};
}

const std::string currentDateTime() {
  time_t     now = time(0);
  struct tm  tstruct;
  char       buf[80];
  tstruct = *localtime(&now);
  // Visit http://en.cppreference.com/w/cpp/chrono/c/strftime for more information about date/time format
  strftime(buf, sizeof(buf), "%Y-%m-%d.%X", &tstruct);
  return buf;
}

void flush(std::string type, std::vector<std::string>& collector, bool isFinal=false) {
  // std::string extra;
  // unsigned cap_job_bench = 1;

  // extra = "cap-cout";

  // if (isFinal)
  //   extra += "-final";
  // std::ofstream out("../benchs/" + extra + "-" + currentDateTime() + ".csv");
  // assert(out.is_open());
  // out << "query" << ","
  //     << "size" << ","
  //     << "optimal_ccap_cost,dpconv_time [mus]" << ","
  //     << "optimal_cout_cost"
  //     << std::endl;
  // for (auto elem : collector) {
  //   out << elem << "\n";
  // }
}