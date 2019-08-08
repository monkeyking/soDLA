package nvdla

import chisel3._
import chisel3.experimental._
import chisel3.util._

//Implementation overview of ping-pong register file.

class NV_NVDLA_MCIF_WRITE_ig extends Module {
    val io = IO(new Bundle {
        //general clock
        val nvdla_core_clk = Input(Clock())
        val pwrbus_ram_pd = Input(UInt(32.W))

        //mcif2noc
        val mcif2noc_axi_aw_awvalid = Output(Bool())
        val mcif2noc_axi_aw_awready = Input(Bool())
        val mcif2noc_axi_aw_awid = Output(UInt(8.W))
        val mcif2noc_axi_aw_awlen = Output(UInt(4.W))
        val mcif2noc_axi_aw_awaddr = Output(UInt(conf.NVDLA_MEM_ADDRESS_WIDTH.W))

        val mcif2noc_axi_w_wvalid = Output(Bool())
        val mcif2noc_axi_w_wready = Input(Bool())
        val mcif2noc_axi_w_wdata = Output(UInt(conf.NVDLA_PRIMARY_MEMIF_WIDTH.W))
        val mcif2noc_axi_w_wstrb = Output(UInt(conf.NVDLA_PRIMARY_MEMIF_STRB.W))
        val mcif2noc_axi_w_wlast = Output(Bool())

        //noc2mcif
        val noc2mcif_axi_b_bvalid = Input(Bool())
        val noc2mcif_axi_b_bready = Output(Bool())
        val noc2mcif_axi_b_bid = Input(UInt(8.W))

        //WDMA_NAME

        //reg2dp
        val reg2dp_wr_os_cnt = Input(UInt(8.W))
        })
    withClock(io.nvdla_core_clk){
}}  
