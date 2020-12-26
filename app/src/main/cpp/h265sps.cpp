#include <iostream>
#include <memory.h>
#include <cstdlib>
#include <bh.h>
typedef signed char int8;

typedef unsigned char   uint8;

typedef unsigned short uint16;

typedef unsigned long uint32;
typedef signed char int8;
typedef signed short int16;
typedef signed long int32;
struct vc_params_t
{
    long width,height;
    int8 profile, level;
    int8 nal_length_size;
    void clear()
    {
        memset(this, 0, sizeof(*this));
    }
};

class NALBitstream
{
public:
    NALBitstream() : m_data(NULL), m_len(0), m_idx(0), m_bits(0), m_byte(0), m_zeros(0)
    {

    };
    NALBitstream(void * data, int len)
    {
        Init(data, len);
    };
    void Init(void * data, int len)
    {
        m_data = (LPBYTE)data;
        m_len = len;
        m_idx = 0;
        m_bits = 0;
        m_byte = 0;
        m_zeros = 0;
    };
    BYTE GetBYTE()
    {
        if ( m_idx >= m_len )
            return 0;
        BYTE b = m_data[m_idx++];
        if ( b == 0 )
        {
            m_zeros++;
            if ( (m_idx < m_len) && (m_zeros == 2) && (m_data[m_idx] == 0x03) )
            {
                m_idx++;
                m_zeros=0;
            }
        }
        else  m_zeros = 0;

        return b;
    };

    UINT32 GetBit()
    {
        if (m_bits == 0)
        {
            m_byte = GetBYTE();
            m_bits = 8;
        }
        m_bits--;
        return (m_byte >> m_bits) & 0x1;
    };

    UINT32 GetWord(int bits)
    {
        UINT32 u = 0;
        while ( bits > 0 )
        {
            u <<= 1;
            u |= GetBit();
            bits--;
        }
        return u;
    };

    UINT32 GetUE()
    {
        int zeros = 0;
        printf("index :%d\n",   m_idx );
        printf("data:%x\n",  m_data[m_idx]);
        while (m_idx < m_len && GetBit() == 0 ) zeros++;
        UINT32 value=GetWord(zeros) + ((1 << zeros) - 1);
        printf("value:%x\n",  value);
        return value;
    };

    INT32 GetSE()
    {
        UINT32 UE = GetUE();
        bool positive = UE & 1;
        INT32 SE = (UE + 1) >> 1;
        if ( !positive )
        {
            SE = -SE;
        }
        return SE;
    };
private:
    LPBYTE m_data;
    int m_len;
    int m_idx;
    int m_bits;
    BYTE m_byte;
    int m_zeros;
};

bool  ParseSequenceParameterSet(BYTE* data,int size, vc_params_t& params)
{
    if (size < 20)
    {
        return false;
    }
    NALBitstream bs(data, size);
    // seq_parameter_set_rbsp()
   int  test =bs.GetWord(4);// sps_video_parameter_set_id
    int sps_max_sub_layers_minus1 = bs.GetWord(3);
    if (sps_max_sub_layers_minus1 > 6)
    {
        return false;
    }
    test =bs.GetWord(1);
    {
        test =bs.GetWord(2);
        test =bs.GetWord(1);
        test = params.profile = bs.GetWord(5);
        test = bs.GetWord(32);//
        test =bs.GetWord(1);//
        test =bs.GetWord(1);//
        test = bs.GetWord(1);//
        test =bs.GetWord(1);//
        test = bs.GetWord(44);//
       test= params.level   = bs.GetWord(8);// general_level_idc
        uint8 sub_layer_profile_present_flag[6] = {0};
        uint8 sub_layer_level_present_flag[6]   = {0};
        for (int i = 0; i < sps_max_sub_layers_minus1; i++) {
            sub_layer_profile_present_flag[i]= bs.GetWord(1);
            sub_layer_level_present_flag[i]= bs.GetWord(1);
        }
        if (sps_max_sub_layers_minus1 > 0)
        {
            for (int i = sps_max_sub_layers_minus1; i < 8; i++) {
                uint8 reserved_zero_2bits = bs.GetWord(2);
            }
        }
        for (int i = 0; i < sps_max_sub_layers_minus1; i++)
        {
            if (sub_layer_profile_present_flag[i]) {
                test =bs.GetWord(2);
                test = bs.GetWord(1);
                test = bs.GetWord(5);
                test =bs.GetWord(32);
                test = bs.GetWord(1);
                test = bs.GetWord(1);
                test = bs.GetWord(1);
                test = bs.GetWord(1);
                test = bs.GetWord(44);
            }
            if (sub_layer_level_present_flag[i]) {
                test = bs.GetWord(8);// sub_layer_level_idc[i]
            }
        }
    }
    uint32 sps_seq_parameter_set_id= bs.GetUE();
    if (sps_seq_parameter_set_id > 15) {
        return false;
    }
    uint32 chroma_format_idc = bs.GetUE();
    if (sps_seq_parameter_set_id > 3) {
        return false;
    }
    if (chroma_format_idc == 3) {
        test =  bs.GetWord(1);//
    }
    printf("-------------value------------\n");
    params.width  = bs.GetUE(); // pic_width_in_luma_samples
    params.height  = bs.GetUE(); // pic_height_in_luma_samples
    if (bs.GetWord(1)) {
        bs.GetUE();
        bs.GetUE();
        bs.GetUE();
        bs.GetUE();
    }
    uint32 bit_depth_luma_minus8= bs.GetUE();
    uint32 bit_depth_chroma_minus8= bs.GetUE();
    if (bit_depth_luma_minus8 != bit_depth_chroma_minus8) {
        return false;
    }
    //...
    return true;
}
int main() {
    std::cout << "Hello, World!" << std::endl;
    {
        vc_params_t params = {0};
//        BYTE Sps[41] = {0X42,0X01,0X01,0X01,0X60,0X00,0X00,0X03,0X00,0X80,0X00,0X00,0X03,0X00,0X00,
//                        0X03,0X00,0X5D,0XA0,0X02,0X80, 0X80,0X2D,0X16,0X59,0X5E,0X49,0X32,0XB8,0X04,0X00,0X00,0X03,
//                        0X00,0X04,0X00,0X00,0X03,0X00,0X64,0X20};

        BYTE Sps[41] = {0X42 ,0X01 ,0X01 ,0X01 ,0X60 ,0X00 ,0X00 ,0X03 ,0X00 ,0XB0 ,0X00 ,0X00
                        ,0X03 ,0X00 ,0X00 ,0X03 ,0X00 ,0X5A ,0XA0 ,0X04 ,0X42 ,0X00 ,0XF0 ,0X77 ,
                        0XE5 ,0XAE ,0XE4 ,0XC9 ,0X2E ,0XA5 ,0X20 ,0XA0 ,0XC0 ,0XC0 ,0X5D ,0XA1 ,0X42 ,0X50};
        ParseSequenceParameterSet(Sps,41,params);
        printf("%d-%d-%d\n",params.width,params.height,params.level);
        system("pause");
        return 0;
    }
    return 0;
}
