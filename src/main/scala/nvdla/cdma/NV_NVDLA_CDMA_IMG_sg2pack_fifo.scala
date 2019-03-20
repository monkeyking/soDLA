// package nvdla

// import chisel3._
// import chisel3.experimental._
// import chisel3.util._
// import chisel3.iotesters.Driver

// class NV_NVDLA_CDMA_IMG_sg2pack_fifo extends Module {
//     val io = IO(new Bundle {
//         //clk
//         val clk = Input(Clock())

//         val wr_ready = Output(Bool())
//         val wr_req = Input(Bool())
//         val wr_data = Input(UInt(11.W))
//         val rd_ready = Input(Bool())
//         val rd_req = Output(Bool())
//         val rd_data = Output(UInt(11.W))

//         val pwrbus_ram_pd = Input(UInt(32.W))
//     })
//     //     
//     //          ┌─┐       ┌─┐
//     //       ┌──┘ ┴───────┘ ┴──┐
//     //       │                 │
//     //       │       ───       │          
//     //       │  ─┬┘       └┬─  │
//     //       │                 │
//     //       │       ─┴─       │
//     //       │                 │
//     //       └───┐         ┌───┘
//     //           │         │
//     //           │         │
//     //           │         │
//     //           │         └──────────────┐
//     //           │                        │
//     //           │                        ├─┐
//     //           │                        ┌─┘    
//     //           │                        │
//     //           └─┐  ┐  ┌───────┬──┐  ┌──┘         
//     //             │ ─┤ ─┤       │ ─┤ ─┤         
//     //             └──┴──┘       └──┴──┘
//     withClock(io.clk){
//     // Master Clock Gating (SLCG)
//     //
//     // We gate the clock(s) when idle or stalled.
//     // This allows us to turn off numerous miscellaneous flops
//     // that don't get gated during synthesis for one reason or another.
//     //
//     // We gate write side and read side separately. 
//     // If the fifo is synchronous, we also gate the ram separately, but if
//     // -master_clk_gated_unified or -status_reg/-status_logic_reg is specified, 
//     // then we use one clk gate for write, ram, and read.
//     //
//     val clk_mgated_enable = Wire(Bool())
//     val clk_mgate = Module(new NV_CLK_gate_power)
//     clk_mgate.io.clk := io.clk
//     clk_mgate.io.clk_en := clk_mgated_enable
//     val clk_mgated = clk_mgate.io.clk_gated

//     ////////////////////////////////////////////////////////////////////////
//     // WRITE SIDE                                                        //
//     ////////////////////////////////////////////////////////////////////////
//     val wr_reserving = Wire(Bool())
//     val wr_req_in = RegInit(false.B)    // registered wr_req
//     val wr_data_in = Reg(UInt(11.W))     // registered wr_data
//     val wr_busy_in = RegInit(false.B)   // inputs being held this cycle?
//     io.wr_ready := !wr_busy_in
//     val wr_busy_next = Wire(Bool())     // fwd: fifo busy next?

//     // factor for better timing with distant wr_req signal
//     val wr_busy_in_next_wr_req_eq_1 = wr_busy_next
//     val wr_busy_in_next_wr_req_eq_0 = (wr_req_in && wr_busy_next) && !wr_reserving
//     val wr_busy_in_next = Mux(io.wr_req, wr_busy_in_next_wr_req_eq_1, wr_busy_in_next_wr_req_eq_0)
//     val wr_busy_in_int = Wire(Bool())

//     wr_busy_in := wr_busy_in_next
//     when(!wr_busy_in_int){
//         wr_req_in := io.wr_req && !wr_busy_in
//     }
//     when(!wr_busy_in && io.wr_req){
//         wr_data_in := io.wr_data
//     }

//     val wr_busy_int = withClock(clk_mgated){RegInit(false.B)}  // copy for internal use
//     wr_reserving := wr_req_in && !wr_busy_int   // reserving write space?

//     val wr_popping = withClock(clk_mgated){RegInit(false.B)}       // fwd: write side sees pop?
//     val wr_count = withClock(clk_mgated){RegInit("b0".asUInt(8.W))} // write-side count
//     val wr_count_next_wr_popping = Mux(wr_reserving, wr_count, wr_count-1.U)
//     val wr_count_next_no_wr_popping = Mux(wr_reserving, wr_count+1.U, wr_count)
//     val wr_count_next = Mux(wr_popping, wr_count_next_wr_popping, wr_count_next_no_wr_popping)

//     val wr_count_next_no_wr_popping_is_128 = (wr_count_next_no_wr_popping === 128.U)
//     val wr_count_next_is_128 = Mux(wr_popping, false.B, wr_count_next_no_wr_popping_is_128)
//     val wr_limit_muxed = Wire(UInt(8.W))    // muxed with simulation/emulation overrides
//     val wr_limit_reg = wr_limit_muxed
//     wr_busy_next := wr_count_next_is_128 ||(wr_limit_reg =/= 0.U && (wr_count_next >= wr_limit_reg))
//     wr_busy_in_int := wr_req_in && wr_busy_int

//     wr_busy_int := wr_busy_next
//     when(wr_reserving ^ wr_popping){
//         wr_count := wr_count_next
//     }

//     val wr_pushing = wr_reserving // data pushed same cycle as wr_req_in

//     //
//     // RAM
//     //  

//     val wr_adr = withClock(clk_mgated){RegInit("b0".asUInt(7.W))}
//     val wr_adr_next = wr_adr + 1.U
//     when(wr_pushing){
//         wr_adr := wr_adr_next
//     }
//     val rd_popping = Wire(Bool())

//     val rd_adr = withClock(clk_mgated){RegInit("b0".asUInt(7.W))}   // read address this cycle
//     val ram_we = wr_pushing && (wr_count > 0.U || !rd_popping)      // note: write occurs next cycle
//     val ram_iwe = !wr_busy_in && io.wr_req
    

//     // Adding parameter for fifogen to disable wr/rd contention assertion in ramgen.
//     // Fifogen handles this by ignoring the data on the ram data out for that cycle.
//     val ram = Module(new NV_NVDLA_CSC_SG_wt_fifo_flopram_rwsa_4x20())
//     ram.io.clk := io.clk
//     ram.io.clk_mgated := clk_mgated
//     ram.io.pwrbus_ram_pd := io.pwrbus_ram_pd
//     ram.io.di := io.wr_data
//     ram.io.iwe := ram_iwe
//     ram.io.we := ram_we
//     ram.io.wa := wr_adr
//     ram.io.ra := Mux(wr_count === 0.U, 4.U, Cat(0.U, rd_adr))
//     io.rd_data := ram.io.dout
    

//     val rd_adr_next_popping = rd_adr + 1.U
//     when(rd_popping){
//         rd_adr := rd_adr_next_popping
//     }

//     //
//     // SYNCHRONOUS BOUNDARY
//     //
//     wr_popping := rd_popping    // let it be seen immediately
//     val rd_pushing = wr_pushing // let it be seen immediately
//     //
//     // SYNCHRONOUS BOUNDARY
//     //
//     wr_popping := rd_popping    
//     val rd_pushing = withClock(clk_mgated){RegNext(wr_pushing, false.B)} 

//     //
//     // READ SIDE
//     //
//     val rd_req_p = withClock(clk_mgated){RegInit(false.B)}  // data out of fifo is valid
//     val rd_req_int = withClock(clk_mgated){RegInit(false.B)} // internal copy of rd_req
//     io.rd_req := rd_req_int
//     rd_popping := rd_req_p && !(rd_req_int && !io.rd_ready)

//     val rd_count_p = withClock(clk_mgated){RegInit("b0".asUInt(8.W))} //read-side fifo count
//     val rd_count_p_next_rd_popping = Mux(rd_pushing, rd_count_p, rd_count_p-1.U)
//     val rd_count_p_next_no_rd_popping = Mux(rd_pushing, rd_count_p + 1.U, rd_count_p)
//     val rd_count_p_next = Mux(rd_popping, rd_count_p_next_rd_popping, rd_count_p_next_no_rd_popping)

//     val rd_count_p_next_rd_popping_not_0 = rd_count_p_next_rd_popping =/= 0.U
//     val rd_count_p_next_no_rd_popping_not_0 = rd_count_p_next_no_rd_popping =/= 0.U
//     val rd_count_p_next_not_0 = Mux(rd_popping, rd_count_p_next_rd_popping_not_0, rd_count_p_next_no_rd_popping_not_0)

//     rd_enable := ((rd_count_p_next_not_0) && ((~rd_req_p) || rd_popping));  // anytime data's there and not stalled

    
//     when(rd_pushing || rd_popping){
//         rd_count_p := rd_count_p_next
//         rd_req_p := rd_count_p_next_not_0
//     }
    
//     val rd_req_next = (rd_req_p || (rd_req_int && !io.rd_ready))

//     rd_req_int := rd_req_next

//     io.rd_data := rd_data_p
//     ore := rd_popping

//     clk_mgated_enable := ((wr_reserving || wr_pushing || wr_popping || 
//                          (wr_req_in && !wr_busy_int) || (wr_busy_int =/= wr_busy_next)) || 
//                          (rd_pushing || rd_popping || (rd_req_int && io.rd_ready)) || (wr_pushing))

//     wr_limit_muxed := "d0".asUInt(8.W)

    
// }}

// class NV_NVDLA_CDMA_IMG_sg2pack_fifo_flopram_rwsa_128x11 extends Module{
//   val io = IO(new Bundle{
//         val clk = Input(Clock())
//         val clk_mgated = Input(Clock())

//         val di = Input(UInt(11.W))
//         val iwe = Input(Bool())
//         val we = Input(Bool())
//         val wa = Input(UInt(7.W))
//         val ra = Input(UInt(8.W))
//         val dout = Output(UInt(11.W))

//         val pwrbus_ram_pd = Input(UInt(32.W))

//   })  
// withClock(io.clk){
//     val di_d = Reg(UInt(11.W))
//     when(io.iwe){
//         di_d := io.di
//     }
//     val ram_ff = Seq.fill(128)(withClock(io.clk_mgated){Reg(UInt(11.W))}) :+ Wire(UInt(11.W))
//     when(io.we){
//         for(i <- 0 to 127){
//             when(io.wa === i.U){
//                 ram_ff(i) := di_d
//             }
//         }
//         ram_ff(128) := di_d
//     }    
//     io.dout := MuxLookup(io.ra, "b0".asUInt(11.W), 
//         (0 to 128) map { i => i.U -> ram_ff(i)} )
// }}






    
// object NV_NVDLA_CDMA_IMG_sg2pack_fifoDriver extends App {
//   chisel3.Driver.execute(args, () => new NV_NVDLA_CDMA_IMG_sg2pack_fifo())
// }